package com.voipgrid.vialer.calling;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class Dialer extends LinearLayout implements KeyPadView.OnKeyPadClickListener, NumberInputView.OnInputChangedListener {

    private Listener listener;

    @BindView(R.id.number_input_edit_text) NumberInputView mNumberInput;
    @BindView(R.id.key_pad_view) KeyPadView mKeypad;
    @BindView(R.id.button_call) ImageButton mCallButton;

    private Unbinder unbinder;
    private final boolean showExitButton;
    private final boolean showCallButton;
    private final boolean showRemoveButton;

    public Dialer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.view_dialer, this);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Dialer);
        showExitButton = a.getBoolean(R.styleable.Dialer_show_exit_button, false);
        showRemoveButton = a.getBoolean(R.styleable.Dialer_show_remove_button, false);
        showCallButton = a.getBoolean(R.styleable.Dialer_show_call_button, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        unbinder = ButterKnife.bind(this);
        mNumberInput.setOnInputChangedListener(this);
        mKeypad.setOnKeyPadClickListener(this);
        if (showExitButton) {
            mNumberInput.enableExitButton();
        }
        if (showRemoveButton) {
            mNumberInput.enableRemoveButton();
        }
        if (!showCallButton) {
            mCallButton.setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unbinder.unbind();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setNumber(String number) {
        mNumberInput.setNumber(number);
    }

    @Override
    public void onKeyPadButtonClick(String digit, String chars) {
        mNumberInput.add(digit);
        mNumberInput.setCorrectFontSize();
        listener.digitWasPressed(digit);
    }

    public String getNumber() {
        return mNumberInput.getNumber();
    }

    @Override
    public void onInputChanged(String number) {
        listener.numberWasChanged(number);
    }

    @Override
    public void exitButtonWasPressed() {
        listener.exitButtonWasPressed();
    }

    public interface Listener {
        void numberWasChanged(String number);
        void digitWasPressed(String digit);
        void exitButtonWasPressed();
    }
}
