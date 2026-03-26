package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.*;
import com.axelor.apps.seo.db.repo.BookingRepository;
import com.axelor.apps.seo.db.repo.RegistrationRepository;
import com.axelor.apps.seo.db.repo.RequisiteRepository;
import com.axelor.apps.seo.integrations.bakai.dto.GenerateQrRequestDto;
import com.axelor.apps.seo.integrations.bakai.dto.GenerateQrResponseDto;
import com.axelor.apps.seo.integrations.bakai.helper.BakaiGateway;
import com.axelor.apps.seo.rest.exception.BusinessException;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.PaymentService;
import com.axelor.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.UUID;

import static com.axelor.apps.seo.utils.StatusConstants.*;

public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RequisiteRepository requisiteRepository;
    private final BakaiGateway gatewayClient;
    private final CrudService crudService;
    private final HttpServletRequest httpRequest;
    private final RegistrationRepository registrationRepository;
    private final BookingRepository bookingRepository;

    @Inject
    public PaymentServiceImpl(RequisiteRepository requisiteRepository, BakaiGateway gatewayClient,
                              CrudService crudService, HttpServletRequest httpRequest,
                              RegistrationRepository registrationRepository, BookingRepository bookingRepository) {
        this.requisiteRepository = requisiteRepository;
        this.gatewayClient = gatewayClient;
        this.crudService = crudService;
        this.httpRequest = httpRequest;
        this.registrationRepository = registrationRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Response generateQr(String requisiteNumber) {
        String clientIp = extractClientIp();

        Requisite requisite = requisiteRepository.all()
                .filter("self.requisiteNumber = :num")
                .bind("num", requisiteNumber)
                .fetchOne();
        if (requisite == null) {
            throw new BusinessException("Requisite not found: " + requisiteNumber, 404);
        }

        CustomsCheckpoint ccp = getCcp(requisite);
        if (ccp == null) {
            throw new BusinessException("CCP not found for requisite: " + requisiteNumber, 400);
        }

        String operationID = PROJECT_PREFIX +"-"+ UUID.randomUUID();
        Payment payment = createPayment(ccp, operationID, requisiteNumber, clientIp);
        crudService.persistObject(payment);

        try {
            GenerateQrRequestDto requestDto = buildQrRequest(ccp, operationID);
            GenerateQrResponseDto qrResponse = gatewayClient.generateQr(requestDto);

            if (qrResponse.getQrImage() != null && !qrResponse.getQrImage().isEmpty()) {
                payment.setQrImage(qrResponse.getQrImage());
                payment.setQrLink(qrResponse.getQrLink());
                payment.setQrImageWithFrame(qrResponse.getQrImageWithFrame());
                payment.setStatus(PAYMENT_STATUS_SUCCESS);
            } else {
                payment.setStatus(PAYMENT_STATUS_ERROR);
            }
            crudService.persistObject(payment);

            return Response.ok(qrResponse).build();

        } catch (BusinessException e) {
            payment.setStatus(PAYMENT_STATUS_ERROR);
            crudService.persistObject(payment);
            throw e;
        }
    }

    @Override
    public String buildPaymentPageHtml(String requisiteNumber) {
        Requisite requisite = requisiteRepository.all()
                .filter("self.requisiteNumber = :num")
                .bind("num", requisiteNumber)
                .fetchOne();

        String plateNo = "—";
        String driver = "—";
        String ccpName = "—";
        int ttlSeconds = 180; // по умолчанию 3 минуты

        if (requisite != null && requisite.getRegistration() != null) {
            Registration reg = requisite.getRegistration();
            if (reg.getId() != null) {
                reg = registrationRepository.find(reg.getId());
            }
            if (reg != null) {
                plateNo = reg.getPlateNo() != null ? reg.getPlateNo() : "—";
                driver = reg.getFullNameDriver() != null ? reg.getFullNameDriver() : "—";
                ccpName = reg.getCcp() != null && reg.getCcp().getCcpName() != null ? reg.getCcp().getCcpName() : "—";
                // Берём TTL из CCP если настроено
                if (reg.getCcp() != null && reg.getCcp().getQrTtl() != null) {
                    ttlSeconds = convertTtlToSeconds(reg.getCcp().getQrTtl(), reg.getCcp().getQrTtlUnits());
                }
            }
        }

        String qrImageBase64 = null;
        String qrLink = null;
        boolean qrAvailable = false;

        try {
            Response qrResponse = generateQr(requisiteNumber);
            GenerateQrResponseDto qrDto = (GenerateQrResponseDto) qrResponse.getEntity();

            if (qrDto != null) {
                qrImageBase64 = qrDto.getQrImage();
                qrLink = qrDto.getQrLink();
                qrAvailable = (qrImageBase64 != null && !qrImageBase64.trim().isEmpty())
                        || (qrLink != null && !qrLink.trim().isEmpty());
            }
        } catch (Exception e) {
            LOGGER.warn("Gateway unavailable for requisite: {}, error: {}", requisiteNumber, e.getMessage());
            qrAvailable = false;
        }
        return renderHtml(requisiteNumber, plateNo, driver, ccpName, qrAvailable, qrImageBase64, qrLink, ttlSeconds);
    }

    private CustomsCheckpoint getCcp(Requisite requisite) {
        if (requisite.getBooking() != null) {
            Booking booking = requisite.getBooking();
            if (booking.getId() != null) {
                booking = bookingRepository.find(booking.getId());
            }
            return booking != null ? booking.getCcp() : null;
        }
        if (requisite.getRegistration() != null) {
            Registration registration = requisite.getRegistration();
            if (registration.getId() != null) {
                registration = registrationRepository.find(registration.getId());
            }
            return registration != null ? registration.getCcp() : null;
        }
        throw new BusinessException("Requisite has no linked booking or registration", 400);
    }

    private Payment createPayment(CustomsCheckpoint ccp, String operationID, String requisiteNumber, String clientIp) {
        Payment payment = new Payment();
        payment.setAccountNo(ccp.getAccountNo());
        payment.setCurrencyId(ccp.getCurrencyId());
        payment.setAmount(BigDecimal.valueOf(ccp.getPriceForBooking()));
        payment.setQrTtlUnits(ccp.getQrTtlUnits());
        payment.setQrTtl(ccp.getQrTtl());
        payment.setOperationID(operationID);
        payment.setOperationType(PAYMENT_OPERATION_QR_REQUEST);
        payment.setStatus(PAYMENT_STATUS_PROCESSED);
        payment.setRequisite(requisiteNumber);
        payment.setIpAddress(clientIp);
        payment.setCcp(ccp);
        return payment;
    }

    private GenerateQrRequestDto buildQrRequest(CustomsCheckpoint ccp, String operationID) {
        GenerateQrRequestDto requestDto = new GenerateQrRequestDto();
        requestDto.setAccountNo(ccp.getAccountNo());
        requestDto.setCurrencyId(ccp.getCurrencyId());
        requestDto.setAmount(ccp.getPriceForBooking().doubleValue());
        requestDto.setOperationID(operationID);
        requestDto.setQrTtlUnits(ccp.getQrTtlUnits());
        requestDto.setQrTtl(ccp.getQrTtl());
        requestDto.setPrefix(PROJECT_PREFIX);
        return requestDto;
    }

    private String extractClientIp() {
        try {
            String ip = httpRequest.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
            ip = httpRequest.getHeader("X-Real-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            return httpRequest.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String renderHtml(String requisiteNumber, String plateNo, String driver, String ccpName, boolean qrAvailable,
                              String qrImageBase64, String qrLink, int ttlSeconds) {

        String titleText       = I18n.get("Payment");                                                                   //Переводы
        String timeRemaining   = I18n.get("Time remaining");
        String labelRequisite  = I18n.get("Requisite");
        String labelPlateNo    = I18n.get("PlateNO");
        String labelDriver     = I18n.get("Full name driver");
        String labelCcp        = I18n.get("CCP");
        String scanPrompt      = I18n.get("Scan the QR code with your app to complete the payment");
        String qrNotAvailable  = I18n.get("QR code will be available when the payment gateway is connected");
        String expiredTitle    = I18n.get("QR Code Expired");
        String expiredHint     = I18n.get("Please close this tab and generate a new QR code.");
        String openLinkText    = I18n.get("Open payment link");
        String scanQrToPay     = I18n.get("Scan QR code to pay");

        StringBuilder sb = new StringBuilder(4096);

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"").append(I18n.getBundle()).append("\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"utf-8\">\n");
        sb.append("  <title>").append(escapeHtml(titleText)).append(" - ").append(escapeHtml(requisiteNumber)).append("</title>\n");
        sb.append("  <link rel=\"icon\" href=\"/seo/img/axelor.png\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<style>\n");

        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; }\n");                                            //CSS
        sb.append("body { font-family: 'Segoe UI', Arial, sans-serif; display: flex; justify-content: center;\n");
        sb.append("align-items: center; min-height: 100vh; margin: 0; background: #f0f2f5; padding: 20px; }\n");
        sb.append(".card { background: white; border-radius: 16px; padding: 40px; width: 100%;\n");
        sb.append("box-shadow: 0 4px 20px rgba(0,0,0,0.08); }\n");
        sb.append(".header { text-align: center; margin-bottom: 24px; }\n");
        sb.append(".icon { font-size: 56px; margin-bottom: 16px; }\n");
        sb.append(".title { font-size: 22px; color: #1a1a1a; font-weight: 600; }\n");
        sb.append(".timer { margin: 0 0 24px 0; padding: 12px; background: #e8f4fd; border-radius: 10px; text-align: center; }\n");
        sb.append(".timer-text { font-size: 14px; color: #495057; }\n");
        sb.append(".timer-count { font-size: 28px; font-weight: 700; color: #0d6efd; font-family: 'Courier New', monospace; }\n");
        sb.append(".timer-warning { color: #dc3545 !important; }\n");
        sb.append(".timer-expired { background: #f8d7da; }\n");
        sb.append(".content { display: flex; gap: 32px; align-items: flex-start; }\n");
        sb.append(".left { flex: 1; min-width: 0; }\n");
        sb.append(".right { flex: 0 0 400px; display: flex; flex-direction: column; align-items: center; }\n");
        sb.append(".info { text-align: left; background: #f8f9fa; border-radius: 10px; padding: 16px; }\n");
        sb.append(".row { padding: 10px 4px; border-bottom: 1px solid #e9ecef; display: flex; justify-content: space-between;\n");
        sb.append("  align-items: center; }\n");
        sb.append(".row:last-child { border-bottom: none; }\n");
        sb.append(".label { color: #6c757d; font-weight: 600; font-size: 14px; }\n");
        sb.append(".value { color: #1a1a1a; font-size: 14px; font-weight: 500; }\n");
        sb.append(".qr-section { text-align: center; }\n");
        sb.append(".qr-title { font-size: 16px; color: #495057; margin-bottom: 16px; font-weight: 600; }\n");
        sb.append(".qr-image { max-width: 380px; width: 100%; height: auto; border-radius: 8px; }\n");
        sb.append(".qr-link { display: inline-block; margin-top: 12px; color: #0d6efd; text-decoration: none;\n");
        sb.append("font-size: 14px; padding: 8px 20px; border: 1px solid #0d6efd; border-radius: 8px;\n");
        sb.append("transition: all 0.2s; }\n");
        sb.append(".qr-link:hover { background: #0d6efd; color: white; }\n");
        sb.append(".status-unavailable { background: #fff3cd; color: #856404; padding: 16px; border-radius: 10px;\n");
        sb.append("margin-top: 16px; font-size: 14px; line-height: 1.5; text-align: center; }\n");
        sb.append(".status-ready { background: #d1e7dd; color: #0f5132; padding: 12px; border-radius: 10px;\n");
        sb.append("margin-top: 16px; font-size: 13px; line-height: 1.5; }\n");

        sb.append("@media (max-width: 700px) {\n");
        sb.append("  .content { flex-direction: column; }\n");
        sb.append("  .right { flex: none; width: 100%; }\n");
        sb.append("  .card { max-width: 480px; }\n");
        sb.append("}\n");
        sb.append("@media (min-width: 701px) {\n");
        sb.append("  .card { max-width: 900px; }\n");
        sb.append("}\n");

        sb.append("</style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");

        sb.append("<div class=\"card\" id=\"mainCard\">\n");

        sb.append("  <div class=\"header\">\n");                                                                        // Header
        sb.append(qrAvailable ? "    <div class=\"icon\">&#128179;</div>\n" : "    <div class=\"icon\">&#9203;</div>\n");
        sb.append("    <div class=\"title\">").append(escapeHtml(titleText)).append("</div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"timer\" id=\"timerBlock\">\n");                                                       // Timer
        sb.append("    <div class=\"timer-text\">").append(escapeHtml(timeRemaining)).append("</div>\n");
        sb.append("    <div class=\"timer-count\" id=\"timerCount\">--:--</div>\n");
        sb.append("  </div>\n");

        sb.append("  <div class=\"content\">\n");

        sb.append("    <div class=\"left\">\n");                                                                        // Left — информация
        sb.append("      <div class=\"info\">\n");
        sb.append("        <div class=\"row\"><span class=\"label\">").append(escapeHtml(labelRequisite)).append("</span><span class=\"value\">").append(escapeHtml(requisiteNumber)).append("</span></div>\n");
        sb.append("        <div class=\"row\"><span class=\"label\">").append(escapeHtml(labelPlateNo)).append("</span><span class=\"value\">").append(escapeHtml(plateNo)).append("</span></div>\n");
        sb.append("        <div class=\"row\"><span class=\"label\">").append(escapeHtml(labelDriver)).append("</span><span class=\"value\">").append(escapeHtml(driver)).append("</span></div>\n");
        sb.append("        <div class=\"row\"><span class=\"label\">").append(escapeHtml(labelCcp)).append("</span><span class=\"value\">").append(escapeHtml(ccpName)).append("</span></div>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");

        sb.append("    <div class=\"right\">\n");                                                                       // Right — QR

        if (qrAvailable) {
            sb.append("      <div class=\"qr-section\">\n");
            sb.append("        <div class=\"qr-title\">").append(escapeHtml(scanQrToPay)).append("</div>\n");

            if (qrImageBase64 != null && !qrImageBase64.trim().isEmpty()) {
                sb.append("        <img class=\"qr-image\" src=\"data:image/png;base64,").append(qrImageBase64).append("\" alt=\"QR Code\"/>\n");
            }

            if (qrLink != null && !qrLink.trim().isEmpty()) {
                sb.append("        <br/><a class=\"qr-link\" href=\"").append(qrLink).append("\" target=\"_blank\">").append(escapeHtml(openLinkText)).append("</a>\n");
            }

            sb.append("        <div class=\"status-ready\">").append(escapeHtml(scanPrompt)).append("</div>\n");
            sb.append("      </div>\n");
        } else {
            sb.append("      <div class=\"status-unavailable\">&#9888; ").append(escapeHtml(qrNotAvailable)).append("</div>\n");
        }

        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n");

        sb.append("<script>\n");                                                                                        //JavaScript
        sb.append("  var totalSeconds = ").append(ttlSeconds).append(";\n");
        sb.append("  var timerCount = document.getElementById('timerCount');\n");
        sb.append("  var timerBlock = document.getElementById('timerBlock');\n");
        sb.append("  var mainCard = document.getElementById('mainCard');\n\n");

        sb.append("  var expiredTitle = '").append(escapeJs(expiredTitle)).append("';\n");
        sb.append("  var expiredHint  = '").append(escapeJs(expiredHint)).append("';\n\n");

        sb.append("  function updateTimer() {\n");
        sb.append("    if (totalSeconds <= 0) {\n");
        sb.append("      timerCount.textContent = '00:00';\n");
        sb.append("      timerBlock.classList.add('timer-expired');\n");
        sb.append("      onExpired();\n");
        sb.append("      return;\n");
        sb.append("    }\n");
        sb.append("    var minutes = Math.floor(totalSeconds / 60);\n");
        sb.append("    var seconds = totalSeconds % 60;\n");
        sb.append("    timerCount.textContent = (minutes < 10 ? '0' : '') + minutes + ':' + (seconds < 10 ? '0' : '') + seconds;\n");
        sb.append("    if (totalSeconds <= 30) {\n");
        sb.append("      timerCount.classList.add('timer-warning');\n");
        sb.append("    }\n");
        sb.append("    totalSeconds--;\n");
        sb.append("    setTimeout(updateTimer, 1000);\n");
        sb.append("  }\n\n");

        sb.append("  function onExpired() {\n");
        sb.append("    window.onbeforeunload = null;\n");
        sb.append("    if (window.parent && window.parent !== window) {\n");
        sb.append("      window.parent.onbeforeunload = null;\n");
        sb.append("    }\n");
        sb.append("    mainCard.innerHTML = '<div style=\"display:flex;flex-direction:column;justify-content:center;align-items:center;padding:60px;text-align:center;\">' +\n");
        sb.append("      '<div style=\"font-size:64px;margin-bottom:16px;\">⏰</div>' +\n");
        sb.append("      '<div style=\"font-size:22px;color:#dc3545;font-weight:600;margin-bottom:12px;\">' + expiredTitle + '</div>' +\n");
        sb.append("      '<div style=\"font-size:14px;color:#6c757d;\">' + expiredHint + '</div>' +\n");
        sb.append("      '</div>';\n");
        sb.append("  }\n\n");

        sb.append("  window.onbeforeunload = null;\n");
        sb.append("  updateTimer();\n");
        sb.append("</script>\n");

        sb.append("</body></html>");

        return sb.toString();
    }

    private int convertTtlToSeconds(Integer ttl, String ttlUnits) {
        if (ttl == null || ttl <= 0) return 180;
        if (ttlUnits == null) return ttl;
        switch (ttlUnits.toLowerCase()) {
            case "minutes": return ttl * 60;
            case "hours": return ttl * 3600;
            case "seconds": return ttl;
            default: return ttl * 60;                                                                                   // по умолчанию минуты
        }
    }

    private static String escapeHtml(String s) {                                                                        //Экранирование для HTML (замена опасных символов)
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 32);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&#39;");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeJs(String s) {                                                                          //Экранирование для JavaScript-строк (в одинарных кавычках)
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}