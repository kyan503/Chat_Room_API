import { useState } from "react";

const BASE_URL = "http://localhost:8081";

async function authFetch(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: "Lỗi kết nối hệ thống" }));
    throw new Error(err.message || `Lỗi HTTP ${res.status}`);
  }
  return res.json();
}

export default function Auth({ onAuthSuccess, showToast }) {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState(""); // Khớp chuẩn AuthDto.RegisterRequest

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (isLogin) {
        const data = await authFetch("/api/auth/login", { username, password });
        localStorage.setItem("token", data.token);
        showToast("✓ Đăng nhập thành công!");
        onAuthSuccess(data.token);
      } else {
        // Gửi chính xác: username, password, email lên Back-end
        await authFetch("/api/auth/register", { username, password, email });
        showToast("✓ Đăng ký thành công! Vui lòng đăng nhập.");
        setIsLogin(true);
        setPassword("");
      }
    } catch (err) {
      showToast(`❌ Lỗi: ${err.message}`);
    }
  };

  return (
    <div className="auth-wrapper">
      <form onSubmit={handleSubmit} className="auth-form">
        <h2>{isLogin ? "ĐĂNG NHẬP" : "ĐĂNG KÝ HỆ THỐNG"}</h2>
        
        <div className="input-group">
          <input 
            type="text" 
            placeholder="Tên đăng nhập (username)" 
            value={username} 
            onChange={e => setUsername(e.target.value)} 
            required 
            className="auth-input" 
          />
        </div>
        
        <div className="input-group">
          <input 
            type="password" 
            placeholder="Mật khẩu" 
            value={password} 
            onChange={e => setPassword(e.target.value)} 
            required 
            className="auth-input" 
          />
        </div>

        {!isLogin && (
          <div className="input-group">
            <input 
              type="email" 
              placeholder="Địa chỉ Email (email)" 
              value={email} 
              onChange={e => setEmail(e.target.value)} 
              required 
              className="auth-input" 
            />
          </div>
        )}

        <button type="submit" className="auth-button">
          {isLogin ? "KẾT NỐI NGAY" : "TẠO TÀI KHOẢN MỚI"}
        </button>

        <p className="auth-switch-text">
          {isLogin ? "Chưa có tài khoản? " : "Đã có tài khoản? "}
          <span onClick={() => { setIsLogin(!isLogin); setPassword(""); }} className="link-text">
            {isLogin ? "Đăng ký ngay" : "Quay lại Đăng nhập"}
          </span>
        </p>
      </form>
    </div>
  );
}