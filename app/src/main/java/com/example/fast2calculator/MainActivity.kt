package com.example.fast2calculator

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.fast2calculator.model.History
import java.lang.NumberFormatException

class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy {
        findViewById(R.id.expressionTextView)
    }

    private val resultTextView : TextView by lazy {
        findViewById(R.id.resultTextView)
    }

    private val historyLayout: View by lazy {
        findViewById(R.id.historyLayout)
    }

    private val historyLinearLayout : LinearLayout by lazy {
        findViewById(R.id.historyLinearLayout)
    }

    lateinit var db: AppDataBase

    private var isOperator = false
    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDataBase::class.java,
            "historyDB"
        ).build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun buttonClicked(v: View) {
        when(v.id) {
            R.id.btn_zero -> numberButtonClicked("0")
            R.id.btn_one -> numberButtonClicked("1")
            R.id.btn_two -> numberButtonClicked("2")
            R.id.btn_three -> numberButtonClicked("3")
            R.id.btn_four -> numberButtonClicked("4")
            R.id.btn_five -> numberButtonClicked("5")
            R.id.btn_six -> numberButtonClicked("6")
            R.id.btn_seven -> numberButtonClicked("7")
            R.id.btn_eight -> numberButtonClicked("8")
            R.id.btn_nine -> numberButtonClicked("9")
            R.id.btn_plus -> operatourButtonClicked("+")
            R.id.btn_minus -> operatourButtonClicked("-")
            R.id.btn_multi -> operatourButtonClicked("*")
            R.id.btn_divider -> operatourButtonClicked("/")
            R.id.btn_modulo -> operatourButtonClicked("%")
        }
    }

    private fun numberButtonClicked(number: String) {

        if (isOperator) {
            expressionTextView.append(" ")
        }
        isOperator = false
        val expressionText = expressionTextView.text.split(" ")
        //????????? ???????????? ?????? ?????????????????? 15?????? ????????????
        if(expressionText.isNotEmpty() && expressionText.last().length >= 15) {
            Toast.makeText(this,"15?????? ????????? ????????? ??? ????????????.",Toast.LENGTH_SHORT).show()
            return
        } else if(expressionText.last().isEmpty() && number == "0") {
            Toast.makeText(this,"0??? ?????? ?????? ??? ??? ????????????.",Toast.LENGTH_SHORT).show()
        }
        expressionTextView.append(number)

        // resultTextView??? ??????????????? ?????? ????????? ????????? ?????? ??????
        resultTextView.text = calculateExpression()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun operatourButtonClicked(operator: String) {
        if (expressionTextView.text.isEmpty()) {
            return
        }
        when {
            isOperator -> {
                val text = expressionTextView.text.toString()
                expressionTextView.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this,"???????????? ??? ?????? ????????? ??? ????????????..",Toast.LENGTH_SHORT).show()
            }
            else -> {
                expressionTextView.append(" ${operator}")
            }
        }
        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length -1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        expressionTextView.text = ssb

        isOperator = true
        hasOperator = true
    }
    fun clearButtonClicked(v: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v: View) {
        historyLayout.isVisible = true
        historyLinearLayout.removeAllViews()
        // ???????????? ?????? ????????????
        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach{
                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row,null,false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()

    }

    fun closeHistoryButtonClicked(v: View) {
        // ???????????? ???????????? ???????????? ?????????
        historyLayout.isVisible = false
    }
    fun historyClearBUttonClicked(v: View) {
        // ???????????? ?????? ?????? ??????
        historyLinearLayout.removeAllViews()
        // ????????? ?????? ?????? ??????
        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()


    }



    fun resultButtonClicked(v: View) {
        val expressionTexts = expressionTextView.text.split(" ")

        if(expressionTextView.text.isEmpty() || expressionTexts.size ==1) {
            return
        }

        if (expressionTexts.size != 3 && hasOperator) {
            Toast.makeText(this,"?????? ???????????? ?????? ???????????????.",Toast.LENGTH_SHORT).show()
            return
        }
        if (!expressionTexts[0].isNumber() || !expressionTexts[2].isNumber()) {
            Toast.makeText(this,"????????? ??????????????????.",Toast.LENGTH_SHORT).show()
            return
        }
        val expressionText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        //DB??? ???????????? ??????
        Thread(Runnable {
            db.historyDao().insertHistory(History(null,expressionText,resultText))
        }).start()

        resultTextView.text = ""
        expressionTextView.text = resultText

        isOperator = false
        hasOperator = false
    }

    //????????? ????????? resultTextView??? ?????????
    private fun calculateExpression(): String {
        val expressionTexts = expressionTextView.text.split(" ")

        if (hasOperator.not() || expressionTexts.size != 3) {
            return ""
        } else if(!expressionTexts[0].isNumber() || !expressionTexts[2].isNumber()) {
            return ""
        }
        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()
        val op = expressionTexts[1]

        return when(op) {
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }
}

fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        return true
    } catch (e: NumberFormatException) {
        false
    }
}