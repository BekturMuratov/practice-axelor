(function () {
    let myApp = angular.module('axelor.ui');

    myApp.formInput('QrImage', {
        init: function (scope) {
            scope.imageUrl = null;
            scope.$watch(function () {
                return scope.getValue();
            }, function (newValue) {
                if (newValue) {
                    scope.imageUrl = 'data:image/png;base64,' + newValue;
                } else {
                    scope.imageUrl = null;
                }
            });
        },
        link: function (scope, element) {
            setTimeout(function () {
                let parent = element.closest('.form-item-container');
                if (parent) {
                    parent.find('input, textarea, .input-append, .display-text').hide();
                }
            }, 100);

            scope.openQrModal = function () {
                if (!scope.imageUrl) return;

                let modalId = 'qrModal_' + scope.$id;
                let existingModal = document.getElementById(modalId);
                if (existingModal) {
                    existingModal.remove();
                }

                let modalHtml = `
                    <div class="qr-overlay" id="${modalId}">
                        <div class="qr-popup">
                            <div class="qr-popup-header">
                                <span class="qr-popup-title">QR Code</span>
                                <span class="qr-popup-close" id="${modalId}_close">&times;</span>
                            </div>
                            <div class="qr-popup-body">
                                <img src="${scope.imageUrl}" class="qr-popup-image"/>
                            </div>
                        </div>
                    </div>
                `;

                document.body.insertAdjacentHTML('beforeend', modalHtml);

                let overlay = document.getElementById(modalId);
                let closeBtn = document.getElementById(modalId + '_close');

                requestAnimationFrame(function () {
                    overlay.classList.add('qr-overlay-visible');
                });

                closeBtn.addEventListener('click', function () {
                    closeModal(overlay);
                });

                overlay.addEventListener('click', function (e) {
                    if (e.target === overlay) {
                        closeModal(overlay);
                    }
                });

                function onEscape(e) {
                    if (e.key === 'Escape') {
                        closeModal(overlay);
                        document.removeEventListener('keydown', onEscape);
                    }
                }
                document.addEventListener('keydown', onEscape);

                function closeModal(el) {
                    el.classList.remove('qr-overlay-visible');
                    setTimeout(function () {
                        if (el && el.parentNode) {
                            el.parentNode.removeChild(el);
                        }
                    }, 300);
                }
            };

            if (!document.getElementById('qr-image-widget-styles')) {
                let style = document.createElement('style');
                style.id = 'qr-image-widget-styles';
                style.innerHTML = `
                    /* Скрыть текстовое поле Base64 */
                    [x-field="qrImage"] .form-item-container > input,
                    [x-field="qrImage"] .form-item-container > textarea,
                    [x-field="qrImage"] .form-item-container > .input-append,
                    [x-field="qrImageWithFrame"] .form-item-container > input,
                    [x-field="qrImageWithFrame"] .form-item-container > textarea,
                    [x-field="qrImageWithFrame"] .form-item-container > .input-append,
                    [x-field="qrImage"] .display-text,
                    [x-field="qrImageWithFrame"] .display-text {
                        display: none !important;
                    }

                    .qr-image-container {
                        text-align: center;
                        padding: 10px;
                    }
                    .qr-image-preview {
                        max-width: 250px;
                        max-height: 250px;
                        cursor: pointer;
                        border: 2px solid #e0e0e0;
                        border-radius: 12px;
                        padding: 8px;
                        background: #fff;
                        transition: all 0.3s ease;
                    }
                    .qr-image-preview:hover {
                        border-color: #4a90d9;
                        box-shadow: 0 6px 20px rgba(74, 144, 217, 0.3);
                        transform: scale(1.02);
                    }
                    .qr-no-image {
                        color: #999;
                        font-style: italic;
                        padding: 20px;
                    }

                    .qr-overlay {
                        position: fixed;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        background: rgba(0, 0, 0, 0.6);
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        z-index: 99999;
                        opacity: 0;
                        transition: opacity 0.3s ease;
                    }
                    .qr-overlay-visible {
                        opacity: 1;
                    }

                    .qr-popup {
                        background: #ffffff;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                        max-width: 600px;
                        width: 90%;
                        max-height: 90vh;
                        overflow: hidden;
                        transform: scale(0.9);
                        transition: transform 0.3s ease;
                    }
                    .qr-overlay-visible .qr-popup {
                        transform: scale(1);
                    }

                    .qr-popup-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        padding: 16px 24px;
                        border-bottom: 1px solid #f0f0f0;
                        background: #fafafa;
                    }
                    .qr-popup-title {
                        font-size: 18px;
                        font-weight: 600;
                        color: #333;
                    }
                    .qr-popup-close {
                        font-size: 28px;
                        color: #999;
                        cursor: pointer;
                        line-height: 1;
                        padding: 0 4px;
                        border-radius: 6px;
                        transition: all 0.2s ease;
                    }
                    .qr-popup-close:hover {
                        color: #e74c3c;
                        background: #ffeaea;
                    }

                    .qr-popup-body {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 32px;
                        background: #fff;
                    }
                    .qr-popup-image {
                        max-width: 100%;
                        max-height: 70vh;
                        border: 2px solid #f0f0f0;
                        border-radius: 12px;
                        padding: 16px;
                        background: #fff;
                        object-fit: contain;
                    }
                `;
                document.head.appendChild(style);
            }
        },
        template: `
            <div class="qr-image-container">
                <img ng-if="imageUrl"
                     ng-src="{{imageUrl}}"
                     class="qr-image-preview"
                     ng-click="openQrModal()"
                     title="Click to enlarge"/>
                <p ng-if="!imageUrl" class="qr-no-image">QR code not available</p>
            </div>
        `
    });
}());