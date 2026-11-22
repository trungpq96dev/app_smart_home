package vn.smarthome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.ui.shell.AppShell
import vn.smarthome.ui.theme.SmartHomeTheme

/**
 * Kiem thu khoi dong, chay tren JVM bang Robolectric - khong can may that.
 *
 * Muc dich KHONG phai kiem tra giao dien dep hay xau (viec do phai nhin tren
 * may that), ma la bat loi chet app: thieu icon, chia cho 0, lay phan tu ngoai
 * mang, ve cung am... Nhung thu chi lo ra luc chay chu khong lo luc bien dich.
 *
 * `qualifiers` dat dung kich thuoc vung ve cua Tab A7 Lite nam ngang.
 *
 * LUU Y ve dong ho: sidebar va khung camera co vong lap `while(true) delay(...)`.
 * Neu de dong ho test tu chay thi `waitForIdle` se cho mai mai, nen phai tat
 * autoAdvance. Day la cach lam chuan khi co animation/vong lap vo han.
 */
@RunWith(AndroidJUnit4::class)
// sdk = 34 vi Robolectric 4.13 chua ho tro API 35 (du app compile voi 35).
@Config(sdk = [34], qualifiers = "w1006dp-h601dp-land-tvdpi")
class ShellSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun start() {
        rule.mainClock.autoAdvance = false
        rule.setContent { SmartHomeTheme { AppShell() } }
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun trang_chu_hien_ra() {
        start()
        rule.onNodeWithText("Nhà thông minh của bạn").assertIsDisplayed()
        rule.onNodeWithText("Phòng khách").assertIsDisplayed()
        rule.onNodeWithText("Đèn: Bật").assertIsDisplayed()
    }

    @Test
    fun bam_dieu_khien_mo_duoc_man_chi_tiet_phong() {
        start()
        rule.onNodeWithText("Điều khiển").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Đèn phòng").assertIsDisplayed()
        rule.onNodeWithText("Quạt trần").assertIsDisplayed()
        rule.onNodeWithText("Điều hòa").assertIsDisplayed()
        rule.onNodeWithText("Chế độ hoạt động").assertIsDisplayed()
    }

    @Test
    fun bam_dong_ho_lich_o_trang_chu_mo_man_lich_am() {
        start()
        // O dong ho / am lich ben phai tieu de Trang chu la loi tat vao man
        // Tien ich. Tim theo chuoi "Âm " chu khong go cung ngay am lich:
        // ngay do doi moi ngay, viet cung la test tu do khi qua nua dem.
        rule.onNode(hasText("Âm ", substring = true)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Tiện ích · Lịch vạn niên").assertIsDisplayed()
    }

    @Test
    fun mo_duoc_man_camera() {
        start()
        rule.onNodeWithText("Camera").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Camera an ninh").assertIsDisplayed()
        rule.onNodeWithText("Chụp ảnh").assertIsDisplayed()
        // KHONG kiem ten camera mau ("Sân vườn", "Cửa chính") o day nua.
        // Tu khi go2rtc co dia chi mac dinh 192.168.0.50, AppShell do go2rtc
        // ngay luc mo app: do xong la danh sach mau bi thay bang danh sach
        // that (o day la rong, vi trong Robolectric khong goi duoc ai).
        // Khang dinh ten camera mau tuc la dua vao viec lan do do CHUA kip
        // xong - test se xanh hay do tuy may nhanh hay cham.
    }

    @Test
    fun man_cai_dat_la_man_that_de_khai_bao_broker() {
        start()
        rule.onNodeWithText("Cài đặt").performClick()
        rule.mainClock.advanceTimeByFrame()
        // Ba duong noi ra ngoai phai CUNG nhin thay duoc trong tab "Dich vu &
        // Ket noi", khong bat cuon hay doi tab moi thay.
        rule.onNodeWithText("Broker EMQX — số điện").assertIsDisplayed()
        rule.onNodeWithText("go2rtc — hình camera").assertIsDisplayed()
        rule.onNodeWithText("Thời tiết — Open-Meteo").assertIsDisplayed()
        // KHONG kiem chu "Chưa khai báo ..." nua: tu khi broker va go2rtc co
        // dia chi mac dinh, trang thai luc mo man la "Đang nối"/"Lỗi" tuy mang,
        // chu khong con la "chua khai bao".
    }

    @Test
    fun tab_dien_nang_khong_bia_so_khi_chua_co_du_lieu() {
        start()
        rule.onNodeWithText("Thống kê").performClick()
        rule.mainClock.advanceTimeByFrame()
        // Chua nhan duoc dien/lichsu => khong duoc hien so mau nao.
        // Dong ly do ben duoi doi theo trang thai duong truyen (chua khai bao /
        // dang noi / loi) nen khong kiem o day; cai phai dung la man hinh noi
        // thang ra rang chua co so, thay vi bay ra bieu do mau.
        rule.onNodeWithText("Chưa có số liệu điện").assertIsDisplayed()
    }

    @Test
    fun muc_thong_ke_o_sidebar_mo_duoc_man_thong_ke() {
        start()
        // Muc "Thống kê" moi o sidebar (duoi Cai dat) phai mo man Thong ke.
        rule.onNodeWithText("Thống kê").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Điện năng tiêu thụ").assertIsDisplayed()
    }

    @Test
    fun mo_duoc_man_thong_ke_va_doi_duoc_tab() {
        start()
        rule.onNodeWithText("Thống kê").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Điện năng tiêu thụ").assertIsDisplayed()

        // Doi sang tab Gas - bieu do va tieu de phai doi theo, khong duoc dung hinh.
        rule.onNodeWithText("Gas").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Mức gas còn lại").assertIsDisplayed()
    }

    @Test
    fun muc_server_o_sidebar_mo_duoc_man_server() {
        start()
        // Muc "Server" moi o sidebar (duoi Cai dat, tren Thong ke) phai mo man Server.
        rule.onNodeWithText("Server").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Máy chủ serverubuntu").assertIsDisplayed()
        rule.onNodeWithText("Tài nguyên").assertIsDisplayed()
    }

    @Test
    fun man_cam_bien_hien_cu_sac_cuktech() {
        start()
        // Cam bien khong con la placeholder: no la man cu sac CUKTECH 10 Ultra,
        // doc tu `cuktech/charger/#`.
        rule.onNodeWithText("Cảm biến").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Củ sạc CUKTECH 10 Ultra").assertIsDisplayed()
    }

    @Test
    fun man_cam_bien_khong_bia_so_khi_chua_co_goi() {
        start()
        rule.onNodeWithText("Cảm biến").performClick()
        rule.mainClock.advanceTimeByFrame()
        // Test chay khong co broker -> CuktechState rong. Man hinh PHAI noi
        // thang la chua co so, tuyet doi khong duoc ve mot cu sac dang ra 85W.
        // Day la luat da ap cho tab Dien nang va nhiet do server.
        rule.onNodeWithText("Chưa có số từ củ sạc").assertIsDisplayed()
        rule.onNodeWithText("CHƯA CÓ SỐ").assertIsDisplayed()
    }

    @Test
    fun man_tu_dong_hoa_hien_the_aquacontrol() {
        start()
        // Thay cho phan kiem cu: Tu dong hoa khong con la placeholder.
        rule.onNodeWithText("Tự động hóa").performClick()
        rule.mainClock.advanceTimeByFrame()
        // "AquaControl" co o ca tieu de the lan nut mo app, nen lay cai dau.
        rule.onAllNodesWithText("AquaControl").onFirst().assertIsDisplayed()
        rule.onNodeWithText("Hệ thống Bơm & Cấp Nước Tự Động").assertIsDisplayed()
    }

    @Test
    fun muc_tien_ich_o_sidebar_mo_duoc_lich_am() {
        start()
        rule.onNodeWithText("Tiện ích").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Tiện ích · Lịch vạn niên").assertIsDisplayed()
        // Lich am tinh tai cho, khong phu thuoc mang, nen hai muc nay luon co.
        // Dung "Tháng Can Chi" chu khong phai "Ngày Can Chi": chu sau co o ca
        // o so lieu tren dau lan dong chi tiet ben phai.
        rule.onNodeWithText("Tháng Can Chi").assertIsDisplayed()
        rule.onNodeWithText("Giờ hoàng đạo").assertIsDisplayed()
    }

    @Test
    fun man_thiet_bi_hien_luoi_the() {
        start()
        rule.onNodeWithText("Thiết bị").performClick()
        rule.mainClock.advanceTimeByFrame()
        // Hien du 6 muc phong lon va kich vao la mo man chi tiet phong.
        listOf("Phòng ngủ 1", "Phòng ngủ 2", "Phòng ngủ 3", "Nhà bếp", "Phòng khách", "Hành lang")
            .forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        rule.onNodeWithText("Hành lang").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Điều khiển hành lang").assertIsDisplayed()
        rule.onNodeWithText("Đèn phòng").assertIsDisplayed()
    }

    // ---------------------------------------------------------------
    // Tro ly giong noi
    // ---------------------------------------------------------------

    @Test
    fun nut_mic_hien_tren_moi_man() {
        start()
        rule.onNodeWithContentDescription("Bật trợ lý giọng nói").assertIsDisplayed()
        // Doi sang man khac thi nut van con - no noi tren khung chu khong
        // thuoc man nao.
        rule.onNodeWithText("Thiết bị").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Bật trợ lý giọng nói").assertIsDisplayed()
    }

    /**
     * Bam mic khi CHUA dien khoa Gemini: phai bao va day sang man Cai dat,
     * tuyet doi khong duoc chet app hay mo mic.
     *
     * Day la duong di that cua lan dau cai app - khong co khoa nao trong
     * DataStore, va ma nguon thi khong nhung san khoa nao.
     */
    @Test
    fun bam_mic_khi_chua_co_khoa_thi_mo_man_cai_dat() {
        start()
        rule.onNodeWithContentDescription("Bật trợ lý giọng nói").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Trợ lý giọng nói").assertIsDisplayed()
    }

    @Test
    fun tab_tro_ly_giong_noi_trong_cai_dat_mo_duoc() {
        start()
        rule.onNodeWithText("Cài đặt").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Trợ lý giọng nói").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Trợ lý giọng nói — Gemini Live").assertIsDisplayed()
        rule.onNodeWithText("Khóa API Gemini").assertIsDisplayed()
        rule.onNodeWithText("Model").assertIsDisplayed()
    }

    /**
     * The thong ke muc dung ve duoc khi CHUA co so lieu nao.
     *
     * Day la trang thai that cua lan dau cai app, va cung la cho de chia cho 0
     * nhat: tran ngay chia cho so giay da dung, trong khi ca hai deu bang 0.
     */
    @Test
    fun the_thong_ke_muc_dung_ve_duoc_khi_chua_co_so() {
        start()
        rule.onNodeWithText("Cài đặt").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Trợ lý giọng nói").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Đã dùng bao nhiêu").assertIsDisplayed()
        rule.onNodeWithText("Hôm nay").assertIsDisplayed()
        // assertExists chu khong phai assertIsDisplayed: o nay nam cuoi cot
        // cau hinh, man chi cao 601dp nen phai cuon moi thay. Van phai ve ra
        // duoc - day la cho de chia cho 0 nhat (tran ngay chia cho so giay da
        // dung, ma lan dau cai app thi ca hai deu bang 0).
        rule.onNodeWithText("Trần mỗi ngày (phút)").assertExists()
    }
}
