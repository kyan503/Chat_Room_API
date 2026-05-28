import { useState, useEffect } from "react";
import Auth from "./Auth";
import Chat from "./Chat";
import "./Auth.css";
import "./Chat.css";

const BASE_URL = "http://localhost:8081";

export default function ChatApp() {
  const [token, setToken] = useState(localStorage.getItem("token"));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState("");

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(""), 3000);
  };

  // PROTECTED ROUTE INTERCEPTOR LOGIC (Đồng bộ hạt nhân danh tính /api/users/me)
 // CƠ CHẾ BẢO VỆ TUYẾN ĐƯỜNG: Đọc dữ liệu trực tiếp từ chữ ký số JWT Token
  useEffect(() => {
    if (token) {
      try {
        // Tách chuỗi JWT thành 3 phần, lấy phần giữa [1] (Payload) và giải mã Base64 bằng atob
        const payloadBase64 = token.split(".")[1];
        const decodedPayload = JSON.parse(atob(payloadBase64));

        // Thiết lập thông tin User lấy từ trường 'sub' (thường lưu username trong Spring Security)
        setUser({
          username: decodedPayload.sub,
          displayName: decodedPayload.sub, // Sử dụng username làm tên hiển thị tạm thời
        });
      } catch (error) {
        console.error("Token bị lỗi cấu trúc hoặc không hợp lệ:", error);
        handleLogout(); // Xóa sạch bộ nhớ nếu token hỏng
      } finally {
        setLoading(false);
      }
    } else {
      setLoading(false);
    }
  }, [token]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    setToken(null);
    setUser(null);
  };

  if (loading) {
    return (
      <div className="auth-wrapper">
        <div style={{ color: "#00ff66", fontWeight: "bold", letterSpacing: "1px", fontSize: "13px" }}>
          ĐANG KHỞI TẠO BẢO MẬT TUYẾN ĐƯỜNG...
        </div>
      </div>
    );
  }

  return (
    <>
      {token && user ? (
        // Tuyến đường được bảo vệ (Protected Route)
        <Chat user={user} onLogout={handleLogout} showToast={showToast} />
      ) : (
        // Tuyến công cộng chặn bên ngoài
        <Auth onAuthSuccess={(newToken) => setToken(newToken)} showToast={showToast} />
      )}
      
      {toast && (
        <div style={{
          position: "fixed", bottom: 20, right: 20, backgroundColor: "#111111",
          border: "1px solid #00ff66", color: "#00ff66", padding: "12px 24px",
          borderRadius: "4px", fontSize: "13px", zIndex: 1000, boxShadow: "0 4px 12px rgba(0,0,0,0.5)"
        }}>
          {toast}
        </div>
      )}
    </>
  );
}