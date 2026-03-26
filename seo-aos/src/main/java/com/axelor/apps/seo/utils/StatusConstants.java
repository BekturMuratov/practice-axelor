package com.axelor.apps.seo.utils;

public interface StatusConstants {
    String PROJECT_PREFIX = "SEO";

    String LOGIN_EVENT_AUTHORIZATION_STATUS_SUCCESS = "success";                                                        //Успешно
    String LOGIN_EVENT_AUTHORIZATION_STATUS_FAILURE = "failure";                                                        //Отказано

    String REGISTRATION_TRANSPORT_TYPE_FIELD_PLATE_NO = "plateNo";                                                      //Гос.номер АТС
    String REGISTRATION_TRANSPORT_TYPE_FIELD_PLATE_NO_TRAILER = "plateNoTrailer";                                       //Гос.номер прицепа
    String REGISTRATION_TRANSPORT_TYPE_FIELD_FULL_NAME_DRIVER = "fullNameDriver";                                       //ФИО водителя

    String REGISTRATION_TYPE_OF_CARGO_NORMAL = "normal";                                                                //Обычный
    String REGISTRATION_TYPE_OF_CARGO_PRIORITY = "priority";                                                            //Приоритетный
    String REGISTRATION_TYPE_OF_CARGO_EMPTY = "empty";                                                                  //Порожний/пустой

    String REGISTRATION_QUEUE_TYPE_SELECT = "seo.registration.queue.type.select";                                       //Тип очереди
    String REGISTRATION_TYPE_OF_CARGO_SELECT = "seo.registration.type.of.cargo.select";                                 //Тип груза
    String REGISTRATION_DECLARATION_TYPE_SELECT = "seo.registration.declaration.type.select";                           //Тип декларации
    String BOOKING_SLOTS_SELECT = "seo.booking.slots.select";                                                           //Слоты бронирования
    String BOOKING_SLOTS_STATUS_SELECT = "seo.booking.slots.status.select";                                             //Статусы слотов
    String BOOKING_STATUS_SELECT = "seo.booking.status.select";                                                         //Статусы бронирования
    String REGISTRATION_STATUS_SELECT = "seo.registration.status.select";                                               //Статусы регистрации

    String REGISTRATION_STATUS_ARRIVED = "arrived" ;                                                                    //Прибыл
    String REGISTRATION_STATUS_FORMULATION = "formulation" ;                                                            //Оформление
    String REGISTRATION_STATUS_PENDING = "pending";                                                                     //В ожидании
    String REGISTRATION_STATUS_CALLED = "called";                                                                       //Вызван
    String REGISTRATION_STATUS_DEPARTED = "departed";                                                                   //Выехал
    String REGISTRATION_STATUS_OFFICIAL_CAR = "officialCar";                                                            //Служебное АТС

    Integer CAMERA_VEHICLE_DIRECTION_TYPE_DOWNWARD = 1;                                                                 //Заезжает на СВХ
    Integer CAMERA_VEHICLE_DIRECTION_TYPE_UPWARD = 2;                                                                   //Выезжает из СВХ

    int TYPE_OF_CAMERA_VEHICLE_DIRECTION_ENTRY = 1;                                                                     //Въезд
    int TYPE_OF_CAMERA_VEHICLE_DIRECTION_EXIT = 2;                                                                      //Выезд
    int TYPE_OF_CAMERA_VEHICLE_DIRECTION_OTHER_DIRECTIONS = 0;                                                          //Другие направления

    String CUSTOMS_ONLINE_PAYMENT_YES = "yes";                                                                          //Да
    String CUSTOMS_ONLINE_PAYMENT_NO = "NO";                                                                            //Нет

    String BOOKING_STATUS_ADVANCE = "advanceBooking";                                                                   //Предварительное бронирование
    String BOOKING_STATUS_BOOKED = "booked";                                                                            //Забронирован
    String BOOKING_STATUS_CANCELLED = "cancelled";                                                                      //Отменен

    String BOOKING_LIVE_QUEUE = "liveQueue";                                                                            //Живая очередь
    String BOOKING_TIMED_QUEUE = "timedQueue";                                                                          //По времени
    String BOOKING_SLOT_STATUS = "FREE";                                                                                //Свободен

    String REQUISITE_TYPE_REGISTRATION = "registration";
    String REQUISITE_TYPE_BOOKING = "booking";

    String REGISTRATION_SORTING_TYPE_EMERGENCY = "1. emergency";                                                        //Экстренные
    String REGISTRATION_SORTING_TYPE_PRIORITY = "2. priority";                                                          //Приоритет
    String REGISTRATION_SORTING_TYPE_TIMED_QUEUE = "3. timedQueue";                                                     //По времени
    String REGISTRATION_SORTING_TYPE_LIVE_QUEUE = "4. liveQueue";                                                       //живая очередь

    String CHART_FINANCIAL_INDICATORS_BY_MONTH = "Financial indicators by month";                                       // Финансовые показатели по месяцам
    String CHART_REGISTRATION_NUMBER_BY_MONTH = "Number of registrations by month";                                     // Количество регистраций по месяцам
    String CHART_FINANCIAL_INDICATORS_BY_DAY = "Financial indicators by day";                                           // Финансовые показатели по дням
    String CHART_REGISTRATION_NUMBER_BY_DAY = "Number of registrations by day";                                         // Количество регистраций по дням
    String PAYMENT_STATUS_SUCCESS = "success";                                                                          //Успешно
    String PAYMENT_STATUS_ERROR = "error";                                                                              //Ошибка
    String PAYMENT_STATUS_PROCESSED = "processed";                                                                      //Обработанный

    String PAYMENT_OPERATION_QR_REQUEST = "qr_request";                                                                 //Запрос на создание QR кода
    String PAYMENT_OPERATION_BANK_NOTIFICATION = "bank_notification";                                                   //Уведомление от банка об платеже

    String PAYMENT_QR_TTL_UNITS_MINUTES = "1";                                                                          //Минуты
    String REGISTRATION_IS_PAID_YES = "yes";                                                                            //Да
}
