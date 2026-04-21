/**
 * Minimal stand-in for the real htmlClientSdk injected by vSphere Client when the
 * plug-in runs inside the client iframe. Lets you load index.html from this dev
 * server without 404 on /api/ui/htmlClientSdk.js.
 *
 * Replace with nothing in production: vCenter serves the real script. For local
 * static opens, keep using this route via `server/index.mjs`.
 */
(function (global) {
  var initialized = false;

  function noop() {}

  global.htmlClientSdk = {
    getProxiedPluginServerOrigin: function () {
      return global.location && global.location.origin ? global.location.origin : "";
    },

    initialize: function (callback) {
      if (initialized) {
        if (typeof callback === "function") callback();
        return;
      }
      initialized = true;
      if (typeof callback === "function") {
        global.setTimeout(callback, 0);
      }
    },

    isInitialized: function () {
      return initialized;
    },

    app: {
      getNavigationData: function () {
        return null;
      },
      getClientLocale: function () {
        return "en-US";
      },
      getTheme: function () {
        return { name: "light" };
      },
      getContextObjects: function () {
        return [];
      },
      getApiEndpoints: function () {
        return {};
      },
      getSessionInfo: function (callback) {
        if (typeof callback === "function") {
          callback({ sessionToken: "" });
        }
      },
      getPluginBackendInfo: function (callback) {
        if (typeof callback === "function") {
          callback({
            allPluginBackendServers: [
              { proxiedBaseUrl: "", type: "local-dev-stub" },
            ],
            backendServersPerVc: {},
          });
        }
      },
      formatDateTime: function (ts) {
        return String(ts);
      },
      navigateTo: noop,
      navigateToClientView: noop,
      navigateToRemotePluginView: noop,
      getRemotePluginNavigationMap: noop,
      getClientInfo: noop,
      refreshPluginItemsState: noop,
    },

    event: {
      onThemeChanged: function (handler) {
        if (typeof handler === "function") handler({ name: "light" });
      },
      onGlobalRefresh: noop,
      onDateTimeFormatChanged: noop,
    },

    modal: {
      open: noop,
      close: noop,
      openConfirmationModal: noop,
      openSharedModal: noop,
      getCustomData: function () {
        return null;
      },
      getSharedModalsMap: noop,
      setOptions: noop,
    },
  };
})(typeof globalThis !== "undefined" ? globalThis : typeof self !== "undefined" ? self : this);
