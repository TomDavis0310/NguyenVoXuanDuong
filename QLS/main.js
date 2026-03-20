document.addEventListener("DOMContentLoaded", function () {
    // Đảm bảo lấy danh mục trước, sau đó mới lấy sách và người dùng
    fetchCategories().then(() => {
        fetchBooks();
        fetchUsers();
        populateCategoryDropdown();
    });

    document.getElementById("btnAddBook").addEventListener("click", addBook);
    document.getElementById("btnAddUser").addEventListener("click", addUser);
    document.getElementById("btnAddCategory").addEventListener("click", addCategory);
});

// Lưu danh sách danh mục để sử dụng
let categories = [];

/* ---------------- SÁCH (PRODUCTS) ---------------- */

function fetchBooks() {
    return fetch("http://localhost:8080/api/products")
        .then(response => response.json())
        .then(data => {
            console.log("Dữ liệu sách từ API:", data); // Debug dữ liệu sách
            displayBooks(data);
        })
        .catch(error => console.error("Lỗi khi lấy sách:", error));
}

function displayBooks(books) {
    const bookList = document.getElementById("bookList");
    bookList.innerHTML = "";
    books.forEach(book => {
        // Tìm tên danh mục dựa trên categoryId
        const category = categories.find(cat => cat.id === book.categoryId);
        const categoryName = category ? category.name : "Không có danh mục";
        console.log(`Sách ID: ${book.id}, CategoryId: ${book.categoryId}, Tên danh mục: ${categoryName}`); // Debug
        
        bookList.innerHTML += `
            <tr>
                <td>${book.id}</td>
                <td>${book.name}</td>
                <td>${book.price}</td>
                <td>${book.description}</td>
                <td>${categoryName}</td>
                <td>
                    <button class="btn btn-warning" onclick="editBook(${book.id}, '${book.name}', ${book.price}, '${book.description}', ${book.categoryId || ''})">Sửa</button>
                    <button class="btn btn-danger" onclick="deleteBook(${book.id})">Xóa</button>
                </td>
            </tr>`;
    });
}

function addBook() {
    const bookId = document.getElementById("bookId").value;
    const name = document.getElementById("bookName").value;
    const price = document.getElementById("bookPrice").value;
    const description = document.getElementById("bookDescription").value;
    const categoryId = document.getElementById("bookCategory").value;

    if (!name || !price) {
        console.error("Tên sách và giá là bắt buộc!");
        return;
    }

    const bookData = {
        id: bookId ? parseInt(bookId) : undefined,
        name: name,
        price: parseFloat(price),
        description: description,
        categoryId: categoryId ? parseInt(categoryId) : undefined // Gửi categoryId
    };

    console.log("Dữ liệu gửi lên API:", bookData); // Debug dữ liệu gửi lên

    const url = bookId ? `http://localhost:8080/api/products/${bookId}` : "http://localhost:8080/api/products";
    const method = bookId ? "PUT" : "POST";

    fetch(url, {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bookData)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`Lỗi khi ${method === "PUT" ? "cập nhật" : "thêm"} sách: ${response.status} - ${text || "Không có chi tiết lỗi"}`);
            });
        }
        return response.text().then(text => {
            return text ? JSON.parse(text) : {};
        });
    })
    .then(() => {
        resetBookForm();
        fetchBooks();
    })
    .catch(error => console.error("Lỗi khi thêm/cập nhật sách:", error.message));
}

function deleteBook(id) {
    fetch(`http://localhost:8080/api/products/${id}`, { method: "DELETE" })
        .then(response => {
            if (!response.ok) throw new Error("Lỗi khi xóa sách");
            fetchBooks();
        })
        .catch(error => console.error("Lỗi khi xóa sách:", error));
}

function editBook(id, name, price, description, categoryId) {
    document.getElementById("bookId").value = id;
    document.getElementById("bookName").value = name;
    document.getElementById("bookPrice").value = price;
    document.getElementById("bookDescription").value = description;
    document.getElementById("bookCategory").value = categoryId || "";
}

function resetBookForm() {
    document.getElementById("bookId").value = "";
    document.getElementById("bookName").value = "";
    document.getElementById("bookPrice").value = "";
    document.getElementById("bookDescription").value = "";
    document.getElementById("bookCategory").value = "";
}

// Điền danh sách danh mục vào dropdown
function populateCategoryDropdown() {
    const dropdown = document.getElementById("bookCategory");
    dropdown.innerHTML = '<option value="">Chọn danh mục</option>';
    categories.forEach(category => {
        dropdown.innerHTML += `<option value="${category.id}">${category.name}</option>`;
    });
}

/* ---------------- NGƯỜI DÙNG (USERS) ---------------- */

function fetchUsers() {
    return fetch("http://localhost:8080/api/users")
        .then(response => response.json())
        .then(data => displayUsers(data))
        .catch(error => console.error("Lỗi khi lấy người dùng:", error));
}

function displayUsers(users) {
    const userList = document.getElementById("userList");
    userList.innerHTML = "";
    users.forEach(user => {
        const dateOfBirth = user.dateOfBirth ? new Date(user.dateOfBirth).toLocaleDateString() : "";
        const createdAt = user.createdAt ? new Date(user.createdAt).toLocaleString() : "";
        
        userList.innerHTML += `
            <tr>
                <td>${user.id}</td>
                <td>${user.name}</td>
                <td>${user.email}</td>
                <td>${user.role}</td>
                <td>${dateOfBirth}</td>
                <td>${user.address || ""}</td>
                <td>${user.phoneNumber || ""}</td>
                <td>${createdAt}</td>
                <td>
                    <button class="btn btn-warning" onclick="editUser(${user.id}, '${user.name}', '${user.email}', '${user.role}', '${user.dateOfBirth}', '${user.address}', '${user.phoneNumber}')">Sửa</button>
                    <button class="btn btn-danger" onclick="deleteUser(${user.id})">Xóa</button>
                </td>
            </tr>`;
    });
}

function addUser() {
    const userId = document.getElementById("userId").value;
    const name = document.getElementById("userName").value;
    const email = document.getElementById("userEmail").value;
    const password = document.getElementById("userPassword").value;
    const role = document.getElementById("userRole").value;
    const dateOfBirth = document.getElementById("userDateOfBirth").value;
    const address = document.getElementById("userAddress").value;
    const phoneNumber = document.getElementById("userPhoneNumber").value;

    // Kiểm tra các trường bắt buộc
    if (!name || !email) {
        console.error("Tên và email là bắt buộc!");
        return;
    }

    // Yêu cầu password khi thêm mới
    if (!userId && !password) {
        console.error("Mật khẩu là bắt buộc khi thêm người dùng mới!");
        return;
    }

    // Nếu sửa và để trống password, gửi giá trị mặc định (vì backend yêu cầu)
    const finalPassword = password || (userId ? "defaultPassword" : undefined);

    const userData = {
        id: userId ? parseInt(userId) : undefined,
        name: name,
        email: email,
        password: finalPassword,
        role: role,
        dateOfBirth: dateOfBirth || null,
        address: address || null,
        phoneNumber: phoneNumber || null
    };

    const url = userId ? `http://localhost:8080/api/users/${userId}` : "http://localhost:8080/api/users";
    const method = userId ? "PUT" : "POST";

    fetch(url, {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(userData)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`Lỗi khi ${method === "PUT" ? "cập nhật" : "thêm"} người dùng: ${response.status} - ${text || "Không có chi tiết lỗi"}`);
            });
        }
        return response.text().then(text => {
            return text ? JSON.parse(text) : {};
        });
    })
    .then(() => {
        resetUserForm();
        fetchUsers();
    })
    .catch(error => console.error("Lỗi khi thêm/cập nhật người dùng:", error.message));
}

function deleteUser(id) {
    fetch(`http://localhost:8080/api/users/${id}`, { method: "DELETE" })
        .then(response => {
            if (!response.ok) throw new Error("Lỗi khi xóa người dùng");
            fetchUsers();
        })
        .catch(error => console.error("Lỗi khi xóa người dùng:", error));
}

function editUser(id, name, email, role, dateOfBirth, address, phoneNumber) {
    document.getElementById("userId").value = id;
    document.getElementById("userName").value = name;
    document.getElementById("userEmail").value = email;
    document.getElementById("userPassword").value = ""; // Để trống password khi chỉnh sửa
    document.getElementById("userRole").value = role;
    document.getElementById("userDateOfBirth").value = dateOfBirth ? new Date(dateOfBirth).toISOString().split("T")[0] : "";
    document.getElementById("userAddress").value = address || "";
    document.getElementById("userPhoneNumber").value = phoneNumber || "";
}

function resetUserForm() {
    document.getElementById("userId").value = "";
    document.getElementById("userName").value = "";
    document.getElementById("userEmail").value = "";
    document.getElementById("userPassword").value = "";
    document.getElementById("userRole").value = "";
    document.getElementById("userDateOfBirth").value = "";
    document.getElementById("userAddress").value = "";
    document.getElementById("userPhoneNumber").value = "";
}

/* ---------------- DANH MỤC (CATEGORIES) ---------------- */

function fetchCategories() {
    return fetch("http://localhost:8080/api/categories")
        .then(response => response.json())
        .then(data => {
            categories = data; // Cập nhật danh sách danh mục
            console.log("Danh sách danh mục:", categories); // Debug danh mục
            displayCategories(data);
            populateCategoryDropdown(); // Cập nhật dropdown khi danh mục thay đổi
        })
        .catch(error => console.error("Lỗi khi lấy danh mục:", error));
}

function displayCategories(categories) {
    const categoryList = document.getElementById("categoryList");
    categoryList.innerHTML = "";
    categories.forEach(category => {
        categoryList.innerHTML += `
            <tr>
                <td>${category.id}</td>
                <td>${category.name}</td>
                <td>${category.description}</td>
                <td>
                    <button class="btn btn-warning" onclick="editCategory(${category.id}, '${category.name}', '${category.description}')">Sửa</button>
                    <button class="btn btn-danger" onclick="deleteCategory(${category.id})">Xóa</button>
                </td>
            </tr>`;
    });
}

function addCategory() {
    const categoryId = document.getElementById("categoryId").value;
    const name = document.getElementById("categoryName").value;
    const description = document.getElementById("categoryDescription").value;

    if (!name) {
        console.error("Tên danh mục là bắt buộc!");
        return;
    }

    const categoryData = {
        id: categoryId ? parseInt(categoryId) : undefined,
        name: name,
        description: description
    };

    const url = categoryId ? `http://localhost:8080/api/categories/${categoryId}` : "http://localhost:8080/api/categories";
    const method = categoryId ? "PUT" : "POST";

    fetch(url, {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(categoryData)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`Lỗi khi ${method === "PUT" ? "cập nhật" : "thêm"} danh mục: ${response.status} - ${text || "Không có chi tiết lỗi"}`);
            });
        }
        return response.text().then(text => {
            return text ? JSON.parse(text) : {};
        });
    })
    .then(() => {
        resetCategoryForm();
        fetchCategories();
    })
    .catch(error => console.error("Lỗi khi thêm/cập nhật danh mục:", error.message));
}

function deleteCategory(id) {
    fetch(`http://localhost:8080/api/categories/${id}`, { method: "DELETE" })
        .then(response => {
            if (!response.ok) throw new Error("Lỗi khi xóa danh mục");
            fetchCategories();
        })
        .catch(error => console.error("Lỗi khi xóa danh mục:", error));
}

function editCategory(id, name, description) {
    document.getElementById("categoryId").value = id;
    document.getElementById("categoryName").value = name;
    document.getElementById("categoryDescription").value = description;
}

function resetCategoryForm() {
    document.getElementById("categoryId").value = "";
    document.getElementById("categoryName").value = "";
    document.getElementById("categoryDescription").value = "";
}