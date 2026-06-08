(() => {
  const form = document.querySelector("form[data-register-form]");
  if (!form) return;

  const username = form.querySelector("input[name='username']");
  const email = form.querySelector("input[name='email']");
  const password = form.querySelector("input[name='password']");
  const confirm = form.querySelector("input[name='confirmPassword']");

  function setErr(input, message) {
    const wrap = input.closest(".field");
    const err = wrap ? wrap.querySelector(".err") : null;
    if (err) {
      err.textContent = message || "";
      err.style.display = message ? "block" : "none";
    }
  }

  function validEmail(v) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
  }

  form.addEventListener("submit", (e) => {
    let ok = true;

    const u = (username?.value || "").trim();
    const em = (email?.value || "").trim();
    const p = password?.value || "";
    const c = confirm?.value || "";

    setErr(username, "");
    setErr(email, "");
    setErr(password, "");
    setErr(confirm, "");

    if (!u || u.length < 3 || u.length > 20) {
      setErr(username, "用户名长度需为 3-20 字符");
      ok = false;
    }
    if (!em || !validEmail(em)) {
      setErr(email, "请输入正确的邮箱格式");
      ok = false;
    }
    if (!p || p.length < 6) {
      setErr(password, "密码长度至少 6 位");
      ok = false;
    }
    if (p !== c) {
      setErr(confirm, "两次密码输入不一致");
      ok = false;
    }

    if (!ok) {
      e.preventDefault();
      if (window.AppToast) window.AppToast.bad("请先修正表单错误");
    }
  });
})();

