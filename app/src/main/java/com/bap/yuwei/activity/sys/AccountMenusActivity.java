package com.bap.yuwei.activity.sys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.bap.yuwei.R;
import com.bap.yuwei.activity.base.BaseActivity;

/**
 * 账户设置
 */
public class AccountMenusActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void  onMenuClick(View v){
        switch (v.getId()){
            case R.id.txt_pwd://密码设置
                startActivity(new Intent(mContext,ResetPwdByOldPwdActivity.class));
                break;
            case R.id.txt_phone://电话设置
                startActivity(new Intent(mContext,ResetPhoneActivity.class));
                break;
            case R.id.txt_vat://增票设置
                startActivity(new Intent(mContext,VatActivity.class));
                break;
            default:break;
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_account_menus;
    }

    @Override
    protected void initView() {

    }
}
