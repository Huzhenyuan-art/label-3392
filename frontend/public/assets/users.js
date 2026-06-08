(function() {
  document.addEventListener('DOMContentLoaded', function() {
    var searchForm = document.querySelector('form[action="/admin/users"]');
    if (searchForm) {
      searchForm.addEventListener('submit', function(e) {
        var usernameInput = searchForm.querySelector('input[name="username"]');
        var emailInput = searchForm.querySelector('input[name="email"]');
        if (usernameInput) {
          usernameInput.value = usernameInput.value.trim();
        }
        if (emailInput) {
          emailInput.value = emailInput.value.trim();
        }
      });
    }
  });
})();
