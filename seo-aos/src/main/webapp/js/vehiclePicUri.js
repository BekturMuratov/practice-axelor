(function () {
    let myApp = angular.module('axelor.ui');

    myApp.formInput('VehicleImage', {
        init: function (scope) {
            scope.imageUrls = [];
            scope.$watch(function () {
                return scope.getValue();
            }, function (newValue) {
                if (newValue) {
                    scope.imageUrls = [newValue];
                } else {
                    scope.imageUrls = [];
                }
            });
        },
        link: function (scope) {
            scope.openImageModal = function (imageUrl) {
                let modal = $('#myModal');
                modal.on('hidden.bs.modal', function () {
                    $('#myModal img').off("pointermove touchmove");
                    modal.find('.img-magnifier-glass').remove();
                });

                modal.find('img').attr('src', imageUrl);
                modal.modal('show');

                setTimeout(function () {
                    initMagnifier(imageUrl);
                }, 500);
            };

            function initMagnifier(imageUrl) {
                let img = document.querySelector('#myModal img');
                let magnifierGlass = document.createElement('DIV');
                magnifierGlass.setAttribute('class', 'img-magnifier-glass');

                img.parentElement.querySelectorAll('.img-magnifier-glass').forEach(glass => glass.remove());

                img.parentElement.insertBefore(magnifierGlass, img);

                magnifierGlass.style.backgroundImage = "url('" + imageUrl + "')";
                magnifierGlass.style.backgroundSize = (img.width * 2) + "px " + (img.height * 2) + "px";

                function moveMagnifier(e) {
                    let pos = getCursorPos(e);
                    let x = pos.x;
                    let y = pos.y;

                    if (x > img.width - (magnifierGlass.offsetWidth / 2)) { x = img.width - (magnifierGlass.offsetWidth / 2); }
                    if (x < magnifierGlass.offsetWidth / 2) { x = magnifierGlass.offsetWidth / 2; }
                    if (y > img.height - (magnifierGlass.offsetHeight / 2)) { y = img.height - (magnifierGlass.offsetHeight / 2); }
                    if (y < magnifierGlass.offsetHeight / 2) { y = magnifierGlass.offsetHeight / 2; }

                    magnifierGlass.style.transform = `translate3d(${x - magnifierGlass.offsetWidth / 2}px, ${y - magnifierGlass.offsetHeight / 2}px, 0)`;
                    magnifierGlass.style.backgroundPosition = "-" + ((x * 2) - magnifierGlass.offsetWidth / 2) + "px -" + ((y * 2) - magnifierGlass.offsetHeight / 2) + "px";
                }

                function getCursorPos(e) {
                    let a, x = 0, y = 0;
                    e = e || window.event;
                    a = img.getBoundingClientRect();
                    x = e.pageX - a.left;
                    y = e.pageY - a.top;
                    x = x - window.pageXOffset;
                    y = y - window.pageYOffset;
                    return { x: x, y: y };
                }

                img.addEventListener("pointermove", moveMagnifier);
                img.addEventListener("touchmove", moveMagnifier);
            }

            let style = document.createElement('style');
            style.innerHTML = `
                .img-magnifier-glass {
                    position: absolute;
                    border: 3px solid rgba(0, 0, 0, 0.5);
                    width: 150px;
                    height: 150px;
                    background-repeat: no-repeat;
                    background-size: 200% 200%;
                    pointer-events: none;
                    will-change: transform;
                    box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.5);
                }

                .modal-header .close {
                    position: relative;
                    right: -10px; 
                    top: -10px; 
                    font-size: 1.5rem;
                    color: red;
                    opacity: 0.9;
                    z-index: 1000;
                }

                .modal-header .close:hover {
                    color: red;
                    opacity: 1;
                }

                .modal-content {
                    padding: 0;
                    margin: 0;
                    border: none;
                    position: relative;
                }

                .modal-lg img {
                    display: block;
                    width: 100%;
                    height: auto;
                    padding: 0;
                    margin: 0;
                    object-fit: contain;
                }
            `;
            document.head.appendChild(style);
        },
        template: `
            <div style="display: flex; flex-flow: row wrap; justify-content: space-around;">
                <img ng-repeat="url in imageUrls track by $index" ng-src="{{url}}"  
                    style="margin-right: 10px; margin-bottom: 10px; max-width: 330px; cursor: pointer;" 
                    ng-click="openImageModal(url)">
            </div>
        `,
        template_readonly: function () {
            return (` 
                <div class="modal fade" id="myModal" style="min-width: 1000px;">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                            </div>
                            <img style="min-width: 1000px; max-height: 800px;">
                        </div>
                    </div>
                </div>
            `);
        }
    });
}());
