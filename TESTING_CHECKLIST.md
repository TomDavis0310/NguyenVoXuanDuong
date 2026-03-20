# Sprint 7 - UAT Checklist

## 1. Shipping Rule
- [ ] Tao don hang subtotal < 1.000.000 -> ship = 30.000
- [ ] Tao don hang 1.000.000 <= subtotal < 2.000.000 -> ship = 15.000
- [ ] Tao don hang subtotal >= 2.000.000 -> ship = 0

## 2. Loyalty Lookup + Voucher
- [ ] Dang nhap USER, vao /loyalty/lookup
- [ ] Nhap SDT hop le, tra cuu ra diem
- [ ] Lay OTP doi voucher
- [ ] Nhap OTP sai -> bao loi
- [ ] Nhap OTP dung -> tao voucher thanh cong
- [ ] Voucher moi hien trong danh sach voucher kha dung

## 3. Use Voucher in Checkout
- [ ] Nhap voucher trong checkout, bam ap dung
- [ ] Neu voucher hop le -> co thong bao da ap dung + giam voucher
- [ ] Dat hang COD/MoMo/VNPay voi voucher -> don luu voucherCode va voucherDiscount
- [ ] Voucher da dung khong duoc dung lai

## 4. VNPay Payment Flow
- [ ] Chon VNPAY trong checkout
- [ ] Redirect den cổng VNPay sandbox
- [ ] Return callback thanh cong -> tao don paymentMethod=VNPAY, paymentStatus=PAID
- [ ] Return callback that bai -> quay lai checkout va hien loi

## 5. Authorization
- [ ] Anonymous truy cap /products/add -> redirect login
- [ ] MANAGER truy cap /products/add -> OK
- [ ] MANAGER truy cap /categories/add -> 403
- [ ] USER truy cap /loyalty/lookup -> OK
- [ ] MANAGER truy cap /loyalty/lookup -> 403

## 6. Existing Flow Regression
- [ ] Dang ky / Dang nhap binh thuong
- [ ] Checkout COD binh thuong
- [ ] MoMo callback tao don PAID
- [ ] Lich su don hang va bien lai hien day du thong tin
