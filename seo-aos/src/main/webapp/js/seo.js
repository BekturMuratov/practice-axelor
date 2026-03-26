/*jshint esversion: 6 */
"use strict";
(function () {

    let activeEventSources = [];

    // Функция для закрытия и очистки всех соединений
    const closeAllEventSources = () => {
        for (let es of activeEventSources) {
            es.close();
        }
        activeEventSources = []; // очищаем массив
    };

    // Интервал, который будет закрывать все соединения каждые 5 минут (300000 мс)
    const intervalId = setInterval(closeAllEventSources, 600000);

    let myApp = angular.module('axelor.ui'); // предполагая, что ваш модуль уже был объявлен ранее
    myApp.controller("formViewOnRefresh", function ($scope) {

        let event = new EventSource('ws/events', {withCredentials: true});
        activeEventSources.push(event); // добавьте это

        event.onerror = function (e) {
            console.log("OnError");
        };

        event.onmessage = function (e) {
            let record = $('[ng-controller=FormViewCtrl]').scope().record;

            const data = JSON.parse(e.data);
            if (record.id === data.id) {
                setTimeout(() => {
                    $scope.onRefresh();
                }, 500);
                console.log("Ura");
            }
        };

        event.onopen = function (e) {
            console.log("OnOpen");
        };

        $scope.$on("$destroy", function () {
            clearInterval(intervalId); // останавливаем интервал
            closeAllEventSources(); // закрыть все соединения
        });

        $scope.$on("$routeChangeSuccess", function (e, current, previous) {
            let edit = "#/ds/seo.registration.action-view/edit";
            let grid = "#/ds/seo.registration.action-view/list";
            let hash = location.hash;

            if (hash.includes(edit)) {
                console.log("edit");
                if (event) {
                    event.close();
                }
                if ($scope.event1) {
                    $scope.event1.close();
                }
                $scope.event1 = new EventSource('ws/events', {withCredentials: true});
                activeEventSources.push($scope.event1); // добавьте это

                $scope.event1.onopen = function (e) {
                    console.log("OnOpen route");
                };

                $scope.event1.onmessage = function (e) {
                    let record = $('[ng-controller=FormViewCtrl]').scope().record;

                    const data = JSON.parse(e.data);
                    if (record.id === data.id) {
                        setTimeout(() => {
                            $scope.onRefresh();
                        }, 500);
                        console.log("Ura");
                    }
                };

            } else if (hash.includes(grid)) {
                if ($scope.event1) {
                    $scope.event1.close();
                }
                console.log("else Grid");
                event.close();
            }
        });
    });
    let eventForGrid;
    window.addEventListener("hashchange", function () {
        let gridHash = "#/ds/seo.registration.action-view/list/1";
        let hash = this.location.hash;
        if (hash === gridHash) {
            $("[ng-controller=GridViewCtrl]").scope().onRefresh();
            eventForGrid = new EventSource('ws/events', {withCredentials: true});
            activeEventSources.push(eventForGrid); // добавьте это

            eventForGrid.onopen = function () {
                console.log("OnOpen hashchange");
            };

            eventForGrid.onmessage = function (e) {
                setTimeout(() => {
                    $("[ng-controller=GridViewCtrl]").scope().onRefresh();
                }, 500);
            };
        } else {
            eventForGrid.close();
        }
    });
    window.addEventListener("load", function () {
        let gridHash = "#/ds/seo.registration.action-view/list/1";
        let hash = this.location.hash;
        if (hash === gridHash) {
            eventForGrid = new EventSource('ws/events', {withCredentials: true});
            activeEventSources.push(eventForGrid); // добавьте это

            eventForGrid.onopen = function () {
                console.log("OnOpen load");
            };

            eventForGrid.onmessage = function (e) {
                setTimeout(() => {
                    $("[ng-controller=GridViewCtrl]").scope().onRefresh();
                }, 500);
            };
        } else {
            eventForGrid.close();
        }
    });
}());