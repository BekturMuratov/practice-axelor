package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.Camera;
import com.axelor.apps.seo.db.CameraOwnerLink;
import com.axelor.apps.seo.db.Registration;
import com.axelor.apps.seo.db.repo.RegistrationRepository;
import com.axelor.apps.seo.service.CameraService;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.utils.sse.SseBroadcasterManager;
import com.axelor.apps.seo.web.dto.RequestCameraData;
import com.axelor.db.JPA;
import com.google.inject.Inject;

import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import static com.axelor.apps.seo.utils.ConnectionUtils.getFileServerPhoto;
import static com.axelor.apps.seo.utils.StatusConstants.*;

public class CameraServiceImpl implements CameraService {
    private static final Logger logger = LoggerFactory.getLogger(CameraServiceImpl.class);

    private final SseBroadcasterManager sseBroadcasterManager;
    private final OutboundSseEvent.Builder eventBuilder;
    private final CrudService crudService;
    private final RegistrationRepository registrationRepository;

    @Inject
    public CameraServiceImpl(SseBroadcasterManager sseBroadcasterManager,
                             Sse sse, CrudService crudService, RegistrationRepository registrationRepository) {
        this.sseBroadcasterManager = sseBroadcasterManager;
        this.eventBuilder = sse.newEventBuilder();
        this.crudService = crudService;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public void saveCameraData(RequestCameraData requestCameraData) {

        JPA.em().clear();
        CameraOwnerLink link = getCcp(requestCameraData.getCameraIndexCode());

        Camera camera = mapRequestToCamera(requestCameraData, link );
        Camera savedCamera = crudService.persistObject(camera);

        Registration registration = createRegistrationRecord(savedCamera, link);
        if (registration != null) {
            savedCamera.setRegistration(registration);
            crudService.persistObject(savedCamera);
        }
        if (shouldExitControl(savedCamera)) {
            registrationOnExit(savedCamera);
        }
    }

    private Camera mapRequestToCamera(RequestCameraData request, CameraOwnerLink link) {
        Camera camera = new Camera();
        try {
            camera.setCrossRecordSyscode(request.getCrossRecordSyscode());
            camera.setCameraIndexCode(request.getCameraIndexCode());

            String originalPlate = Optional.ofNullable(request.getPlateNo()).orElse("");
            String adjustedPlate = originalPlate;
            if (request.getCountry() != null
                    && request.getCountry().equals(73)
                    && originalPlate.matches("^0[1-9]\\d{3}.*")) {
                adjustedPlate = originalPlate.substring(0, 2) + "KG" + originalPlate.substring(2);
            }
            camera.setPlateNo(adjustedPlate);
            camera.setPoint(request.getOwnerName());
            camera.setContact(request.getContact());

            camera.setVehicleColor(Optional.ofNullable(request.getVehicleColor()).map(String::valueOf).orElse("0"));
            camera.setVehicleType(Optional.ofNullable(request.getVehicleType()).map(String::valueOf).orElse("0"));
            camera.setCountry(Optional.ofNullable(request.getCountry()).map(String::valueOf).orElse("0"));

            if (request.getVehicleDirectionType() != null) {
                camera.setInfoVehicleDirectionType(String.valueOf(request.getVehicleDirectionType()));
            } else {
                camera.setInfoVehicleDirectionType("0");
            }

            if (link != null){
                camera.setCcp(link.getCcp());

                // 2. VehicleDirectionType
                try {
                    int typeOfCameraValue = link.getTypeOfCamera() != null
                            ? Integer.parseInt(link.getTypeOfCamera())
                            : 0;
                    switch (typeOfCameraValue) {
                        case TYPE_OF_CAMERA_VEHICLE_DIRECTION_ENTRY:
                        case TYPE_OF_CAMERA_VEHICLE_DIRECTION_OTHER_DIRECTIONS:
                            if (CAMERA_VEHICLE_DIRECTION_TYPE_DOWNWARD.equals(request.getVehicleDirectionType())) {
                                camera.setVehicleDirectionType(String.valueOf(CAMERA_VEHICLE_DIRECTION_TYPE_DOWNWARD));
                            } else if (CAMERA_VEHICLE_DIRECTION_TYPE_UPWARD.equals(request.getVehicleDirectionType())) {
                                camera.setVehicleDirectionType(String.valueOf(CAMERA_VEHICLE_DIRECTION_TYPE_UPWARD));
                            } else {
                                camera.setVehicleDirectionType("0");
                            }
                            break;
                        case TYPE_OF_CAMERA_VEHICLE_DIRECTION_EXIT:
                            if (CAMERA_VEHICLE_DIRECTION_TYPE_DOWNWARD.equals(request.getVehicleDirectionType())) {
                                camera.setVehicleDirectionType(String.valueOf(CAMERA_VEHICLE_DIRECTION_TYPE_UPWARD));
                            } else if (CAMERA_VEHICLE_DIRECTION_TYPE_UPWARD.equals(request.getVehicleDirectionType())) {
                                camera.setVehicleDirectionType(String.valueOf(CAMERA_VEHICLE_DIRECTION_TYPE_DOWNWARD));
                            } else {
                                camera.setVehicleDirectionType("0");
                            }
                            break;
                        default:
                            camera.setVehicleDirectionType("0");
                    }
                } catch (NumberFormatException e) {
                    camera.setVehicleDirectionType("0");
                }

                // 3. CrossTime
                if(link.getUseCurrentTime()){
                    camera.setCrossTime(LocalDateTime.now());
                }else{
                    try {
                        if (request.getCrossTime() != null && !request.getCrossTime().isEmpty()) {
                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(request.getCrossTime());
                            camera.setCrossTime(zonedDateTime.toLocalDateTime());
                        } else {
                            camera.setCrossTime(null);
                        }
                    } catch (DateTimeParseException e) {
                        camera.setCrossTime(null);
                    }
                }
            }

            String vehiclePicPath = request.getVehiclePicPath();
            if (vehiclePicPath != null && !vehiclePicPath.isEmpty()) {
                String adjustedVehiclePicPath = adjustVehiclePicPath(vehiclePicPath);
                camera.setVehiclePicUri(adjustedVehiclePicPath);
            } else {
                logger.warn("VehiclePicPath is null or empty for plateNo: {}", camera.getPlateNo());
                camera.setVehiclePicUri(null);
            }

        } catch (DateTimeParseException e) {
            logger.error("Invalid date format in crossTime: {}", request.getCrossTime(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in mapRequestToCamera method: {}", e.getMessage(), e);
        }
        return camera;
    }

    private String adjustVehiclePicPath(String vehiclePicPath) {
        vehiclePicPath = vehiclePicPath.replaceAll("^/+", "");
        return String.format("%s/%s", getFileServerPhoto(), vehiclePicPath);
    }

    private Registration createRegistrationRecord(Camera camera, CameraOwnerLink link) {
        if (link == null) {
            return null;
        }

        boolean shouldCreateRegistration =
                Boolean.TRUE.equals(link.getCreateRegistration())
                        && "1".equals(camera.getVehicleDirectionType())
                        && !"12".equals(camera.getVehicleType());

        if (!shouldCreateRegistration) {
            return null;
        }
        try {
            Registration registration = getRegistration(camera, link);
            crudService.persistObject(registration);
            sseBroadcasterManager.getBroadcaster().broadcast(
                    eventBuilder
                            .data(Registration.class, registration)
                            .mediaType(MediaType.APPLICATION_JSON_TYPE)
                            .build()
            );
            logger.info("Registration created for plateNo: {}", registration.getPlateNoCameraEntry());
            return registration;
        } catch (OptimisticLockException | StaleStateException e) {
            logger.error("Failed to save registration due to concurrency issue for plateNo={} (syscode={}): {}",
                    camera.getPlateNo(), camera.getCrossRecordSyscode(), e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Failed to save registration for plateNo: {}. Cause: {}", camera.getPlateNo(), e.getMessage());
            return null;
        }
    }

    private Registration getRegistration(Camera camera, CameraOwnerLink link) {
        Registration registration = new Registration();
        registration.setPlateNoCameraEntry(camera.getPlateNo());
        registration.setCreatedOn(camera.getCrossTime());
        registration.setPlateNo(camera.getPlateNo());
        registration.setCrossTimeEntry(camera.getCrossTime());
        registration.setVehiclePicEntry(camera.getVehiclePicUri());
        registration.setRegistrationStatus(REGISTRATION_STATUS_PENDING);
        registration.setQueueType(BOOKING_LIVE_QUEUE);
        registration.setTypeOfCargo(REGISTRATION_TYPE_OF_CARGO_NORMAL);

        if (camera.getCcp() != null) {
            registration.setCcp(camera.getCcp());
        } else if (link.getCcp() != null) {
            registration.setCcp(link.getCcp());
        }
        return registration;
    }

    private CameraOwnerLink getCcp(String cameraIndexCode) {
        try {

            return JPA.em()
                    .createQuery("SELECT col FROM CameraOwnerLink col WHERE col.cameraCode = :cameraCode",
                            CameraOwnerLink.class)
                    .setParameter("cameraCode", cameraIndexCode)
                    .getSingleResult();
        } catch (NoResultException e) {
            logger.warn("CameraOwnerLink not found or CCP is null for cameraCode: {}", cameraIndexCode);
            return null;
        }
    }

    private boolean shouldExitControl(Camera camera) {
        String plateNo = camera.getPlateNo();
        if (plateNo == null || plateNo.trim().isEmpty() || "unknown".equalsIgnoreCase(plateNo.trim())) {
            return false;
        }
        String vehicleType = camera.getVehicleType();
        if ("12".equals(vehicleType) || "3".equals(vehicleType)) {
            return false;
        }
        if (!"2".equals(camera.getVehicleDirectionType())) {
            return false;
        }
        return Optional.ofNullable(getCcp(camera.getCameraIndexCode()))
                .map(CameraOwnerLink::getExitControl)
                .orElse(false);
    }

    private void registrationOnExit(Camera camera) {
        String plateNo = camera.getPlateNo().trim();
        LocalDateTime fromDate = LocalDateTime.now().minusHours(72);

        Registration registration = registrationRepository.all()
                .filter("(self.plateNo = :plateNo OR self.plateNoCameraEntry = :plateNo) " +
                        "AND self.createdOn >= :fromDate " +
                        "AND (:ccp IS NULL OR self.ccp = :ccp) " +
                        "AND self.registrationStatus != 'officialCar' " +
                        "AND self.registrationStatus != 'departed'")
                .bind("plateNo", plateNo)
                .bind("fromDate", fromDate)
                .bind("ccp", camera.getCcp())
                .order("-createdOn")
                .fetchOne();

        if (registration == null) {
            logger.warn("Not found registration with plate: {}, ccp: {}", plateNo, camera.getCcp());
            return;
        }

        registration.setPlateNoCameraExit(plateNo);
        registration.setVehiclePicExit(camera.getVehiclePicUri());
        registration.setCrossTimeExit(camera.getCrossTime());
        registration.setRegistrationStatus(REGISTRATION_STATUS_DEPARTED);
        try {
            crudService.persistObject(registration);
            logger.info("Successfully registered exit for plate: {}, registration id: {}",
                    plateNo, registration.getId());
            camera.setRegistration(registration);
            crudService.persistObject(camera);
        } catch (Exception e) {
            logger.error("Failed to persist exit registration for plate: {}", plateNo, e);
        }
    }
}
