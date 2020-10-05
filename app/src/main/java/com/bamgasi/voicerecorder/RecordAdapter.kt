package com.bamgasi.voicerecorder

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.graphics.Color
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bamgasi.voicerecorder.databinding.RecordItemBinding
import com.bamgasi.voicerecorder.fragment.ListFragment
import com.bamgasi.voicerecorder.model.Records
import kotlinx.android.synthetic.main.record_item.view.*

class RecordAdapter(private val list: ArrayList<Records>,
                    val fragment: ListFragment,
                    val context: Context,
                    private val clickListener:(records: Records) -> Unit):
    RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    var selectedIndex = -1

    class RecordViewHolder(val binding: RecordItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.record_item, parent, false)
        val viewHolder = RecordViewHolder(RecordItemBinding.bind(view))

        return viewHolder.apply {
            itemView.setOnClickListener {
                val record = list[adapterPosition]
                val prevSelectedIndex = selectedIndex
                selectedIndex = adapterPosition
                notifyItemChanged(prevSelectedIndex)
                notifyItemChanged(selectedIndex)
                clickListener.invoke(record)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.binding.recording = list[position]

        holder.binding.nameText.setTextColor(Color.BLACK)
        holder.binding.recordItem.setBackgroundColor(android.R.color.background_light)

        val record = list[position]

        if (selectedIndex == position) {
            holder.binding.nameText.setTextColor(Color.RED)
            holder.binding.recordItem.setBackgroundColor(Color.LTGRAY)
        }

        holder.itemView.iv_more.setOnClickListener {
            val popupMenu = PopupMenu(context, it, Gravity.RIGHT)
            popupMenu.menuInflater.inflate(R.menu.list_more_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.id_rename_file -> {
                        popRenameAlert(list[position], position)
                    }
                    R.id.id_share_file -> {
                        fragment.stopPlayer()

                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "audio/*"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, record.recordUri)
                        context.startActivity(Intent.createChooser(shareIntent, record.recordName))
                    }
                    R.id.id_delete_file -> {
                        fragment.stopPlayer()

                        val builder = AlertDialog.Builder(context)
                        builder.setTitle(R.string.title_file_delete)
                            .setMessage(R.string.message_file_delete)
                            .setPositiveButton(R.string.title_btn_delete) { p0, p1 ->
                                try {
                                    record.recordUri.let {
                                        context.contentResolver.delete(it, null, null)
                                        list.removeAt(position)
                                        selectedIndex = -1
                                        notifyItemRemoved(position)
                                        fragment.resetPlayer()
                                    }
                                }catch (e: Exception) {
                                    Toast.makeText(context, R.string.message_delete_file_fail, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton(R.string.title_btn_cancel) { p0, p1 -> }
                        builder.create()
                        builder.show()
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun popRenameAlert(record: Records, position: Int) {
        fragment.stopPlayer()

        val builder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_layout, null)
        val saveName = view.findViewById<EditText>(R.id.save_name)

        /**
         * 이름변경 시 키보드가 강제로 올라오게끔 한다.
         * 잘 동작하지 않아서 일부러 딜레이를 주었다.
         */
        saveName.setText(record.recordName)
        val manager: InputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        saveName.postDelayed({
            kotlin.run {
                saveName.setSelectAllOnFocus(true)
                saveName.requestFocus()
                manager.showSoftInput(saveName, 0)
            }
        }, 100)

        builder.setView(view)
            .setTitle(R.string.title_file_rename)
            .setPositiveButton(R.string.title_btn_save) { p0, p1 ->
                val sourceName = record.recordName + AppConfig.FILE_EXT
                val destName = saveName.text.toString() + AppConfig.FILE_EXT
                if (sourceName != destName) {
                    // 이름변경의 경우 원본 파일은 그대로 두고 표시 이름만 변경하면 된다.
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, destName)
                        put(MediaStore.Audio.Media.DISPLAY_NAME, destName)
                    }

                    context.contentResolver?.update(record.recordUri, values, null, null)

                    /*val sourceFile = File(saveDir, sourceName)
                    val destFile = File(saveDir, destName)
                    sourceFile.renameTo(destFile)*/

                    record.recordName = saveName.text.toString()
                    //record.recordUri = saveDir+destName

                    list[position] = record
                    notifyItemChanged(position)
                }
            }
            .setNegativeButton(R.string.title_btn_cancel) { p0, p1 -> }
        val dialog = builder.create()
        dialog.show()

        saveName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrEmpty()
            }
        })
    }
}