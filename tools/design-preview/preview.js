/*
 * Preview controls.
 *
 * Small on purpose: it switches the two attributes that the preview's stylesheet
 * keys off (the theme, mirroring the application swapping its token stylesheet,
 * and the rail state) and drives the overlays. It does not style anything - every
 * pixel comes from preview.css, which is generated from the application's own
 * JavaFX stylesheets.
 */
(function () {
  "use strict";

  var params = new URLSearchParams(window.location.search);

  function current(key, fallback) {
    return params.get(key) || document.body.dataset[key] || fallback;
  }

  function apply(key, value) {
    document.body.dataset[key] = value;
    params.set(key, value);
    var query = params.toString();
    history.replaceState(null, "", window.location.pathname + (query ? "?" + query : ""));
    syncButtons();
  }

  function syncButtons() {
    document.querySelectorAll("[data-act]").forEach(function (button) {
      var action = button.dataset.act;
      var pressed = false;
      if (action === "theme:" + document.body.dataset.theme) pressed = true;
      if (action === "rail") pressed = false;
      button.setAttribute("aria-pressed", pressed ? "true" : "false");
    });
    document.querySelectorAll(".segmented-item").forEach(function (segment) {
      var action = segment.dataset.act || "";
      segment.classList.toggle("is-selected", action === "theme:" + document.body.dataset.theme);
    });
    // The rail's collapse control states what it will do, like RailNav does.
    var collapsed = document.body.dataset.rail === "collapsed";
    document.querySelectorAll("[data-icon-expanded]").forEach(function (icon) {
      icon.textContent = collapsed ? icon.dataset.iconCollapsed : icon.dataset.iconExpanded;
    });
    document.querySelectorAll("[data-text-expanded]").forEach(function (label) {
      label.textContent = collapsed ? label.dataset.textCollapsed : label.dataset.textExpanded;
    });
  }

  document.addEventListener("click", function (event) {
    var target = event.target.closest("[data-act]");
    if (!target) return;
    var action = target.dataset.act;
    if (action === "rail") {
      apply("rail", document.body.dataset.rail === "collapsed" ? "expanded" : "collapsed");
    } else if (action.indexOf("theme:") === 0) {
      apply("theme", action.split(":")[1]);
    } else if (action.indexOf("overlay:") === 0) {
      var wanted = action.split(":")[1];
      var showing = document.body.dataset.overlay === wanted;
      apply("overlay", showing ? "none" : wanted);
    } else if (action === "focus") {
      apply("focus", document.body.dataset.focus === "on" ? "off" : "on");
    }
  });

  // Clicking the scrim closes it, exactly as CommandPalette does.
  document.querySelectorAll(".overlay").forEach(function (overlay) {
    overlay.addEventListener("click", function (event) {
      if (event.target === overlay) apply("overlay", "none");
    });
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && document.body.dataset.overlay !== "none") {
      apply("overlay", "none");
    }
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
      event.preventDefault();
      apply("overlay", "palette");
    }
  });

  document.body.dataset.theme = current("theme", "light");
  document.body.dataset.rail = current("rail", "expanded");
  document.body.dataset.overlay = current("overlay", "none");
  document.body.dataset.focus = current("focus", "on");
  document.querySelectorAll(".rail-item, .rail-collapse-button").forEach(function (item) {
    item.classList.toggle("rail-collapsed-item", false);
  });
  syncButtons();
})();
