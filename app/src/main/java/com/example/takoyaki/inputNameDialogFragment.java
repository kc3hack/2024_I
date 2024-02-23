package com.example.takoyaki;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class inputNameDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //ダイアログを作るためのビルダーを作成
        //ビルダーでタイトルやメッセージを設定できる
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //ダイアログのタイトル文を設定
        builder.setTitle(R.string.dialog_title);

        //ダイアログのメッセージ文を設定
        builder.setMessage(R.string.dialog_message);

        DialogClickListener listener = new DialogClickListener();

        //Positive ボタンに表示内容と、リスナを設定
        builder.setPositiveButton(R.string.dialog_button_ok,listener);

        //Negative ボタンに表示内容と、リスナを設定
        builder.setNegativeButton(R.string.dialog_button_cancel,listener);

//        //Neutral ボタンに表示内容と、リスナを設定
//        builder.setNeutralButton(R.string.dialog_button_neutral,listener);

        //設定したダイアログを作成
        AlertDialog dialog = builder.create();

        //ダイアログをリターン
        return dialog;
    }

    private class DialogClickListener implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog,int buttonId){

        }
    }

    
}
