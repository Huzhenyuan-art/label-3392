(function() {
  document.addEventListener('DOMContentLoaded', function() {
    var emailForm = document.getElementById('emailForm');
    var passwordForm = document.getElementById('passwordForm');

    if (emailForm) {
      emailForm.addEventListener('submit', function(e) {
        var emailInput = emailForm.querySelector('input[name="email"]');
        if (emailInput && emailInput.value.trim() === '') {
          e.preventDefault();
          window.AppToast && window.AppToast.bad('邮箱不能为空');
          emailInput.focus();
          return false;
        }
      });
    }

    if (passwordForm) {
      passwordForm.addEventListener('submit', function(e) {
        var oldPwd = passwordForm.querySelector('input[name="oldPassword"]');
        var newPwd = passwordForm.querySelector('input[name="newPassword"]');
        var confirmPwd = passwordForm.querySelector('input[name="confirmPassword"]');

        if (oldPwd && oldPwd.value.trim() === '') {
          e.preventDefault();
          window.AppToast && window.AppToast.bad('请输入旧密码');
          oldPwd.focus();
          return false;
        }

        if (newPwd && newPwd.value.length < 6) {
          e.preventDefault();
          window.AppToast && window.AppToast.bad('新密码长度至少6位');
          newPwd.focus();
          return false;
        }

        if (confirmPwd && newPwd.value !== confirmPwd.value) {
          e.preventDefault();
          window.AppToast && window.AppToast.bad('两次密码输入不一致');
          confirmPwd.focus();
          return false;
        }
      });
    }
  });
})();
