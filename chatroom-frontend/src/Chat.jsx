import { useState, useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const BASE_URL = "http://localhost:8081";

async function apiFetch(path, options = {}) {
  const token = localStorage.getItem("token");
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: "Thao tác thất bại" }));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  
  if (res.status === 204) return null;

  const contentType = res.headers.get("content-type");
  if (contentType && contentType.includes("text")) {
    return res.text(); 
  }
  
  return res.json();
}

export default function Chat({ user, onLogout, showToast }) {
  const [allRooms, setAllRooms] = useState([]);      // Tất cả phòng hệ thống
  const [myRooms, setMyRooms] = useState([]);        // Danh sách phòng TÔI ĐÃ JOIN (Lấy từ /api/rooms/my)
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [roomMembers, setRoomMembers] = useState([]);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Hàm nạp danh sách phòng chuẩn hóa bằng cách gọi song song 2 endpoint của Backend
  const fetchRoomsData = async () => {
    try {
      const [allRoomsRes, myRoomsRes] = await Promise.all([
        apiFetch("/api/rooms"),     // Lấy tất cả phòng công khai
        apiFetch("/api/rooms/my")   // Lấy đích danh phòng mình đã vào
      ]);
      setAllRooms(allRoomsRes);
      setMyRooms(myRoomsRes);
    } catch (err) {
      console.error("Lỗi nạp danh sách phòng công khai hoặc phòng cá nhân:", err);
    }
  };

  useEffect(() => {
    fetchRoomsData();
  }, []);

  // Xử lý khi chủ động ấn nút "Tham gia" ở danh sách khám phá
  const handleJoinNewRoom = async (e, room) => {
    e.stopPropagation(); // Không làm kích hoạt sự kiện click chọn dòng
    try {
      await apiFetch(`/api/rooms/${room.id}/join`, { method: "POST" }); //
      showToast(`✓ Đã tham gia vào phòng ${room.name}`);
      
      // Đưa phòng mới vào danh sách phòng của tôi ngay lập tức
      setMyRooms((prev) => [...prev, room]);
      setSelectedRoom(room);
    } catch (err) {
      showToast(`❌ Không thể tham gia phòng: ${err.message}`);
    }
  };

  // Click vào phòng ở Sidebar: Luôn cho phép mở màn hình chat nếu nằm trong danh sách đã tham gia
  const handleSelectRoom = (room) => {
    const isJoined = myRooms.some((r) => r.id === room.id);
    if (isJoined) {
      setSelectedRoom(room); // Nhảy thẳng vào khung message!
    } else {
      showToast("ℹ Vui lòng nhấn nút 'Tham gia' bên cạnh tên phòng để vào trò chuyện.");
    }
  };

  // Lọc danh sách phòng khám phá (Là những phòng nằm trong hệ thống nhưng mình chưa join)
  const myRoomIds = myRooms.map((r) => r.id);
  const publicRooms = allRooms.filter((r) => !myRoomIds.includes(r.id));

  return (
    <div className="chat-app-layout">
      <div className="chat-sidebar">
        <div className="sidebar-profile">
          <div>
            <div className="profile-display-name">{user?.displayName}</div>
            <div className="profile-username">@{user?.username}</div>
          </div>
          <button onClick={onLogout} className="logout-btn">Đăng xuất</button>
        </div>

        {/* MỤC 1: PHÒNG CỦA TÔI */}
        <div className="sidebar-section-title">
          <span>PHÒNG CỦA TÔI ({myRooms.length})</span>
          <button onClick={() => setShowCreateModal(true)} className="add-room-btn">+</button>
        </div>

        <div className="room-scroll-list">
          {myRooms.map((room) => (
            <div
              key={room.id}
              onClick={() => handleSelectRoom(room)}
              className={`room-list-item ${selectedRoom?.id === room.id ? "is-active" : ""}`}
            >
              <span># {room.name}</span>
              <span className="joined-badge">Đã vào</span>
            </div>
          ))}

          {/* MỤC 2: KHÁM PHÁ PHÒNG KHÁC */}
          {publicRooms.length > 0 && (
            <>
              <div className="sidebar-section-title" style={{ marginTop: 20 }}>
                <span>KHÁM PHÁ PHÒNG KHÁC ({publicRooms.length})</span>
              </div>
              {publicRooms.map((room) => (
                <div
                  key={room.id}
                  onClick={() => handleSelectRoom(room)}
                  className="room-list-item public-room-item"
                >
                  <span># {room.name}</span>
                  <button 
                    onClick={(e) => handleJoinNewRoom(e, room)} 
                    className="sidebar-join-action-btn"
                  >
                    Tham gia
                  </button>
                </div>
              ))}
            </>
          )}
        </div>
      </div>

      <div className="chat-main-content">
        {selectedRoom ? (
          <ChatPanel
            key={selectedRoom.id}
            room={selectedRoom}
            currentUser={user}
            roomMembers={roomMembers}
            setRoomMembers={setRoomMembers}
            showToast={showToast}
            onRoomLeaveOrDelete={() => {
              setSelectedRoom(null);
              fetchRoomsData();
            }}
          />
        ) : (
          <div className="chat-empty-state">
            <div className="empty-icon">//</div>
            <div className="empty-text">Chọn hoặc tạo một phòng chat để bắt đầu</div>
          </div>
        )}
      </div>

      {showCreateModal && (
        <CreateRoomModal
          onClose={() => setShowCreateModal(false)}
          onCreated={(newRoom) => {
            setAllRooms((prev) => [...prev, newRoom]);
            setMyRooms((prev) => [...prev, newRoom]); // Phòng do mình tạo ra mặc định mình là thành viên
            setSelectedRoom(newRoom);
            setShowCreateModal(false);
            showToast("✓ Tạo phòng mới thành công");
          }}
        />
      )}
    </div>
  );
}

function ChatPanel({ room, currentUser, roomMembers, setRoomMembers, showToast, onRoomLeaveOrDelete }) {
  const [messages, setMessages] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);    // Quản lý trang tin nhắn hiện tại
  const [hasMoreMessages, setHasMoreMessages] = useState(false); // Trạng thái xem còn tin nhắn cũ không
  const [input, setInput] = useState("");
  const stompClientRef = useRef(null);
  const scrollRef = useRef(null);
  const isFirstLoadRef = useRef(true); // Cờ kiểm soát cuộn màn hình xuống đáy khi mới mở phòng

  // Hàm tải tin nhắn có hỗ trợ phân trang
  const fetchMessages = (pageNumber, isLoadMore = false) => {
    // Gọi API phân trang tin nhắn từ Backend: mặc định size=20 tin nhắn mỗi trang
    apiFetch(`/api/rooms/${room.id}/messages?page=${pageNumber}&size=20`) //
      .then((data) => {
        // Cú pháp bóc tách dữ liệu từ cấu trúc PageResponse của Spring Boot
        const serverMessages = data.content ? data.content : [];
        
        if (isLoadMore) {
          // Khi tải tin nhắn cũ: Giữ nguyên tin nhắn mới hiện tại, nối các tin cũ (đảo thứ tự) lên đầu dòng thời gian
          setMessages((prev) => [...[...serverMessages].reverse(), ...prev]);
        } else {
          // Khi mở phòng lần đầu: Đảo ngược mảng tin nhắn mới nhất để hiển thị theo thứ tự từ cũ đến mới
          setMessages([...serverMessages].reverse());
        }

        // Kiểm tra điều kiện hasNext: Spring Boot trả về cấu trúc PageResponse, nếu số phần tử nhận được bằng đúng kích thước size thì khả năng cao vẫn còn trang tiếp theo.
        // Hoặc nếu Backend có trường dữ liệu boolean hasNext/last thì bạn có thể đổi thành: setHasMoreMessages(!data.last)
        setHasMoreMessages(serverMessages.length === 20);
        setCurrentPage(pageNumber);
      })
      .catch(() => {
        if (!isLoadMore) setMessages([]);
      });
  };

  useEffect(() => {
    isFirstLoadRef.current = true;
    fetchMessages(0, false); // Luôn tải trang số 0 (Trang tin nhắn mới nhất) khi click chọn phòng

    apiFetch(`/api/rooms/${room.id}/members`) //
      .then(setRoomMembers)
      .catch(() => setRoomMembers([]));
  }, [room.id, setRoomMembers]);

  useEffect(() => {
    // Chỉ tự động cuộn xuống đáy màn hình chat khi mới mở phòng hoặc có tin nhắn mới tinh xuất hiện
    if (isFirstLoadRef.current && messages.length > 0) {
      scrollRef.current?.scrollIntoView({ behavior: "auto" });
      isFirstLoadRef.current = false;
    } else if (!isFirstLoadRef.current) {
      scrollRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  // Hàm xử lý kích hoạt khi người dùng nhấn nút "Tải tin nhắn cũ hơn"
  const handleLoadMore = () => {
    isFirstLoadRef.current = false; // Tắt cờ cuộn để giữ nguyên vị trí view của người dùng
    fetchMessages(currentPage + 1, true); // Gọi trang tiếp theo (cũ hơn)
  };

  // THIẾT LẬP KẾT NỐI WEBSOCKET
  useEffect(() => {
    const token = localStorage.getItem("token");
    const socketUrl = `${BASE_URL}/ws?token=${token}`; //

    const client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        // Lắng nghe kênh phòng chat
        client.subscribe(`/topic/room/${room.id}`, (message) => { //
          if (message.body) {
            const data = JSON.parse(message.body);
            if (data.type === "JOIN" || data.type === "LEAVE") {
              apiFetch(`/api/rooms/${room.id}/members`).then(setRoomMembers).catch(() => {}); //
            }
            // Đẩy tin nhắn real-time mới vào cuối mảng
            setMessages((prev) => [...prev, data]);
          }
        });
        // Bắn tín hiệu gia nhập socket hệ thống
        client.publish({ destination: `/app/room/${room.id}/join` }); //
      },
      onStompError: (f) => showToast(`❌ Lỗi WebSocket: ${f.headers["message"]}`)
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current) stompClientRef.current.deactivate();
    };
  }, [room.id, setRoomMembers]);

  const handleSend = (e) => {
    e.preventDefault();
    if (!input.trim() || !stompClientRef.current?.connected) return;
    stompClientRef.current.publish({
      destination: `/app/chat/${room.id}`, //
      body: JSON.stringify({ content: input })
    });
    setInput("");
  };

  const handleLeave = async () => {
    if (!window.confirm("Bạn muốn rời khỏi phòng này?")) return;
    try {
      await apiFetch(`/api/rooms/${room.id}/leave`, { method: "DELETE" }); //
      showToast("✓ Đã rời phòng");
      onRoomLeaveOrDelete();
    } catch (err) {
      showToast(`❌ Lỗi: ${err.message}`);
    }
  };

  const handleDeleteRoom = async () => {
    if (!window.confirm("BẠN CÓ CHẮC CHẮN MUỐN XÓA HOÀN TOÀN PHÒNG NÀY?")) return;
    try {
      await apiFetch(`/api/rooms/${room.id}`, { method: "DELETE" }); //
      showToast("✓ Đã xóa phòng thành công");
      onRoomLeaveOrDelete();
    } catch (err) {
      showToast(`❌ Không có quyền xóa: ${err.message}`);
    }
  };

  return (
    <div className="panel-inner-container">
      <div className="message-area-wrapper">
        <div className="panel-chat-header">
          <span># {room.name}</span>
          <div className="room-actions-group">
            <button onClick={handleLeave} className="action-room-btn leave-btn">Rời Phòng</button>
            <button onClick={handleDeleteRoom} className="action-room-btn delete-room-btn">Xóa Phòng</button>
          </div>
        </div>
        
        <div className="scrolling-message-list">
          {/* NÚT TẢI TIN NHẮN CŨ: Xuất hiện ở trên cùng nếu hasMoreMessages = true */}
          {hasMoreMessages && (
            <button onClick={handleLoadMore} className="load-more-btn">
              ⬆ Tải tin nhắn cũ hơn
            </button>
          )}

          {messages.map((msg, idx) => {
            const isMe = msg.senderUsername === currentUser.username;
            const isSystem = msg.senderUsername === "system";
            return (
              <div key={idx} className={`chat-msg-row ${isSystem ? "msg-system" : isMe ? "msg-me" : "msg-others"}`}>
                {!isSystem && (
                  <div className="msg-meta-info">
                    {msg.senderDisplayName || msg.senderUsername} <span>• {new Date(msg.sentAt).toLocaleTimeString()}</span>
                  </div>
                )}
                <div className="msg-bubble-text">{msg.content}</div>
              </div>
            );
          })}
          <div ref={scrollRef} />
        </div>

        <form onSubmit={handleSend} className="panel-input-form">
          <input type="text" placeholder="Nhập nội dung tin nhắn..." value={input} onChange={e => setInput(e.target.value)} className="panel-input-field" />
          <button type="submit" className="panel-send-btn">GỬI</button>
        </form>
      </div>

      <div className="member-right-sidebar">
        <div className="member-sidebar-header">Thành viên ({roomMembers.length})</div>
        <div className="member-scroll-box">
          {roomMembers.map((member, idx) => (
            <div key={idx} className="member-card">
              <div className="status-dot-online"></div>
              <div>
                <div className="member-card-name">{member.user?.displayName || member.user?.username}</div>
                <div className="member-card-user">@{member.user?.username}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function CreateRoomModal({ onClose, onCreated }) {
  const [name, setName] = useState("");
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const data = await apiFetch("/api/rooms", { //
        method: "POST",
        body: JSON.stringify({ name }),
      });
      onCreated(data);
    } catch (err) {
      alert(err.message);
    }
  };
  return (
    <div className="modal-screen-overlay">
      <div className="modal-core-box">
        <h3>TẠO PHÒNG CHAT MỚI</h3>
        <form onSubmit={handleSubmit}>
          <input type="text" placeholder="Tên phòng..." value={name} onChange={e => setName(e.target.value)} required className="modal-text-input" />
          <div className="modal-btn-group">
            <button type="button" onClick={onClose} className="modal-action-btn btn-cancel">HỦY</button>
            <button type="submit" className="modal-action-btn btn-confirm">TẠO PHÒNG</button>
          </div>
        </form>
      </div>
    </div>
  );
}