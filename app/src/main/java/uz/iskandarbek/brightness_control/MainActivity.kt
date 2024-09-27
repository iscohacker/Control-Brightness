package uz.iskandarbek.brightness_control

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private var initialY = 0f
    private var dY = 0f
    private var screenBrightness = 128 // Boshlang'ich yorug'lik darajasi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sliderView = findViewById<View>(R.id.slider_view)
        val cardView = findViewById<CardView>(R.id.brightness_card)

        // Ekran sozlamalarini boshqarish ruxsatini tekshirish
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            setUpSlider(sliderView, cardView)
        }
    }

    override fun onResume() {
        super.onResume()

        // Ruxsat berilganini tekshirish
        if (Settings.System.canWrite(this)) {
            val sliderView = findViewById<View>(R.id.slider_view)
            val cardView = findViewById<CardView>(R.id.brightness_card)
            setUpSlider(sliderView, cardView)
        } else {
            Toast.makeText(this, "Ekran yorug'ligini boshqarish uchun ruxsat berishingiz kerak", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpSlider(sliderView: View, cardView: CardView) {
        val resolver: ContentResolver = contentResolver

        // Sliderni teginish orqali harakatlantiramiz
        sliderView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = v.y
                    dY = event.rawY - v.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val newY = event.rawY - dY
                    val maxMoveRange = cardView.height - v.height // Faqat CardView balandligi ichida harakatlanadi
                    v.y = max(0f, min(newY, maxMoveRange.toFloat()))

                    // Slayder ko'tarilgan yoki tushirilgan holatda yorug'likni yangilash
                    updateBrightness(v.y.toInt(), maxMoveRange, resolver)

                    // Slayder yuqoriga ko'tarilganda va pastga tushganda cardning pastki qismidagi rangni o'zgartirish
                    updateCardBackground(cardView, v.y.toInt(), maxMoveRange)
                }
                MotionEvent.ACTION_UP -> {
                    // Slayderdan qo'lni lib tashlaganda yorug'lik darajasi tez o'zgarishi
                    updateBrightness(v.y.toInt(), cardView.height - v.height, resolver)
                }
            }
            true
        }
    }

    // Yorug'likni yangi o'rnatish
    private fun updateBrightness(currentY: Int, maxRange: Int, resolver: ContentResolver) {
        val newBrightness = 255 - (255 * currentY / maxRange)
        screenBrightness = max(0, min(newBrightness, 255)) // Yorug'likni 0-255 oralig'ida saqlash

        // Yorug'lik darajasini tizimga o'rnatish
        try {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, screenBrightness)
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Brightness ni o'rnatishda xatolik", Toast.LENGTH_SHORT).show()
        }
    }

    // CardViewning pastki qismini rangini o'zgartirish
    private fun updateCardBackground(cardView: CardView, currentY: Int, maxRange: Int) {
        // Ko'tarilgan miqdorga qarab cardning pastki qismi rangini moslashtiramiz
        val alphaValue = 255 - (255 * currentY / maxRange) // Ko'tarilgan holatiga qarab rang o'zgarishi
        cardView.setCardBackgroundColor(android.graphics.Color.argb(alphaValue, 0, 0, 0)) // Qora rang shaffof ko'rinishda
    }
}
