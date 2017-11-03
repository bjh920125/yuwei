package com.bap.yuwei.activity.goods;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bap.yuwei.R;
import com.bap.yuwei.activity.base.BaseActivity;
import com.bap.yuwei.adapter.GoodsAdapter;
import com.bap.yuwei.entity.Constants;
import com.bap.yuwei.entity.goods.Goods;
import com.bap.yuwei.entity.goods.Shop;
import com.bap.yuwei.entity.http.AppResponse;
import com.bap.yuwei.entity.http.ResponseCode;
import com.bap.yuwei.util.DateTimeUtil;
import com.bap.yuwei.util.DisplayImageOptionsUtil;
import com.bap.yuwei.util.LogUtil;
import com.bap.yuwei.util.MyApplication;
import com.bap.yuwei.util.StringUtils;
import com.bap.yuwei.util.ThrowableUtil;
import com.bap.yuwei.util.ToastUtil;
import com.bap.yuwei.webservice.GoodsWebService;
import com.githang.statusbar.StatusBarCompat;
import com.github.jdsjlzx.interfaces.OnLoadMoreListener;
import com.github.jdsjlzx.interfaces.OnRefreshListener;
import com.github.jdsjlzx.recyclerview.LRecyclerView;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopGoodsActivity extends BaseActivity {

    private ImageView imgShop,imgHead;
    private TextView txtShopName,txtGoodComment,txtCollectNum,txtGoodsTotal,txtGoodsNews;
    private LRecyclerView gvGoods;
    private Button btnHome;
    private TextView txtAllTitle,txtAllNum,txtNewTitle,txtNewNum;
    private View viewHome,viewAll,viewNew;
    private LinearLayout llFilter;
    private TextView txtMult,txtSell,txtTime,txtPrice;
    private EditText etSearch;


    private List<Goods> mGoods;
    private Shop mShop;
    private GoodsAdapter mGoodsAdapter;
    private LRecyclerViewAdapter mLRecyclerViewAdapter = null;

    private GoodsWebService goodsWebService;

    private int  pageIndex = 1;
    private int orderType=1;
    private String queryTime;
    private String goodsTitle;
    private String categoryNodes;
    private boolean isPriceAsc=false;

    private boolean hasCollected=false;

    private int color;
    private int selectColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        goodsWebService= MyApplication.getInstance().getWebService(GoodsWebService.class);
        mGoods=new ArrayList<>();
        mShop= (Shop) getIntent().getSerializableExtra(Shop.KEY);
        color=getResources().getColor(R.color.lightblack);
        selectColor=getResources().getColor(R.color.colorPrimary);
        mGoodsAdapter = new GoodsAdapter(mContext);
        mLRecyclerViewAdapter = new LRecyclerViewAdapter(mGoodsAdapter);
        gvGoods.setAdapter(mLRecyclerViewAdapter);

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    goodsTitle= StringUtils.getEditTextValue(etSearch);
                    gvGoods.refresh();
                }
                return true;
            }
        });

        gvGoods.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                mGoods.clear();
                mGoodsAdapter.clear();
                pageIndex=1;
                getGoodsList();
            }
        });

        gvGoods.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                pageIndex++;
                getGoodsList();
            }
        });
        getShopCollect();
        gvGoods.refresh();
        initUIWithValue();
    }


    public void shopCollect(View v){
        if(hasCollected){
            cancelShopCollect();
        }else{
            addShopCollect();
        }
    }

    public void onTabClicked(View v){
        viewHome.setVisibility(View.INVISIBLE);
        viewAll.setVisibility(View.INVISIBLE);
        viewNew.setVisibility(View.INVISIBLE);
        llFilter.setVisibility(View.GONE);
        btnHome.setTextColor(color);
        txtAllNum.setTextColor(color);
        txtAllTitle.setTextColor(color);
        txtNewNum.setTextColor(color);
        txtNewTitle.setTextColor(color);
        Drawable drawable= getResources().getDrawable(R.drawable.dianpu);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        btnHome.setCompoundDrawables(null,drawable,null,null);
        goodsTitle=null;
        switch (v.getId()){
            case R.id.btn_home:
                viewHome.setVisibility(View.VISIBLE);
                Drawable drawablefill= getResources().getDrawable(R.drawable.dianpu_fill);
                drawablefill.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                btnHome.setCompoundDrawables(null,drawablefill,null,null);
                btnHome.setTextColor(selectColor);
                queryTime=null;
                break;
            case R.id.rl_all:
                viewAll.setVisibility(View.VISIBLE);
                txtAllNum.setTextColor(selectColor);
                txtAllTitle.setTextColor(selectColor);
                llFilter.setVisibility(View.VISIBLE);
                queryTime=null;
                break;
            case R.id.rl_new:
                viewNew.setVisibility(View.VISIBLE);
                txtNewNum.setTextColor(selectColor);
                txtNewTitle.setTextColor(selectColor);
                queryTime= DateTimeUtil.getNowTimeStr(DateTimeUtil.DATE_MONTH_FORMAT);
                break;
            default:break;
        }
        gvGoods.refresh();
    }


    public void chooseSort(View view){
        txtMult.setTextColor(color);
        txtSell.setTextColor(color);
        txtTime.setTextColor(color);
        txtPrice.setTextColor(color);
        switch (view.getId()) {
            case R.id.txt_mult:
                orderType=1;
                txtMult.setTextColor(selectColor);
                break;
            case R.id.txt_sell:
                orderType=2;
                txtSell.setTextColor(selectColor);
                break;
            case R.id.txt_time:
                orderType=3;
                txtTime.setTextColor(selectColor);
                break;
            case R.id.txt_price:
                if(isPriceAsc){
                    isPriceAsc=false;
                    orderType=4;
                    Drawable drawable= getResources().getDrawable(R.drawable.triangle_up);
                    drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    txtPrice.setCompoundDrawables(null,null,drawable,null);
                }else{
                    isPriceAsc=true;
                    orderType=5;
                    Drawable drawable= getResources().getDrawable(R.drawable.triangle_down);
                    drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    txtPrice.setCompoundDrawables(null,null,drawable,null);
                }
                txtPrice.setTextColor(selectColor);
                break;
            default:
                break;
        }
        gvGoods.refresh();
    }

    private void getGoodsList(){
        Map<String,Object> params=new HashMap<>();
        params.put("categoryNodes",categoryNodes);
        params.put("discountStatus",0);
        params.put("goodsTitle",goodsTitle);
        params.put("orderType",orderType);//1:综合 2：销量 3：时间 4：价格从低到高 5：价格从高到低 6：好评量 7:人气
        params.put("queryTime",queryTime);
        params.put("shopId",mShop.getShopId());
        params.put("stockStatus",0);
        params.put("pageNumber",pageIndex);
        params.put("pageSize",12);
        RequestBody body=RequestBody.create(jsonMediaType,mGson.toJson(params));
        Call<ResponseBody> call=goodsWebService.getShopGoods(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String result=response.body().string();
                    LogUtil.print("result",result);
                    AppResponse appResponse=mGson.fromJson(result,AppResponse.class);
                    if(appResponse.getCode()== ResponseCode.SUCCESS){
                        JSONArray jo=new JSONObject(result).getJSONObject("result").getJSONArray("list");
                        List<Goods> tempList = mGson.fromJson(jo.toString(), new TypeToken<List<Goods>>() {}.getType());
                        if(tempList!=null && tempList.size()>0){
                            mGoodsAdapter.addAll(tempList);
                            mGoodsAdapter.notifyDataSetChanged();
                            gvGoods.refreshComplete(tempList.size());
                        }else{
                            gvGoods.setNoMore(true);
                        }
                    }else{
                        ToastUtil.showShort(mContext,appResponse.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                ToastUtil.showShort(mContext, ThrowableUtil.getErrorMsg(t));
            }
        });
    }

    /**
     * 获取收藏店铺详情
     */
    private void getShopCollect(){
        if(null==mUser) return;
        Call<ResponseBody> call=goodsWebService.getShopCollect(mUser.getUserId(),mShop.getShopId());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String result=response.body().string();
                    LogUtil.print("result",result);
                    AppResponse appResponse=mGson.fromJson(result,AppResponse.class);
                    if(appResponse.getCode()== ResponseCode.SUCCESS){
                        JSONObject jo=new JSONObject(result).getJSONObject("result");
                        if(null != jo){//已收藏
                            hasCollected=true;
                            txtCollectNum.setText("取消 "+mShop.getShopCollectUserTotal()+"人");
                        }
                    }else{
                        ToastUtil.showShort(mContext,appResponse.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                ToastUtil.showShort(mContext, ThrowableUtil.getErrorMsg(t));
            }
        });
    }

    /**
     * 收藏店铺
     */
    private void addShopCollect(){
        Map<String,Object> params=new HashMap<>();
        params.put("shopId",mShop.getShopId());
        params.put("shopName", mShop.getShopName());
        params.put("userId",mUser.getUserId());
        RequestBody body=RequestBody.create(jsonMediaType,mGson.toJson(params));
        Call<ResponseBody> call=goodsWebService.addShopCollect(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String result=response.body().string();
                    LogUtil.print("result",result);
                    AppResponse appResponse=mGson.fromJson(result,AppResponse.class);
                    if(appResponse.getCode()== ResponseCode.SUCCESS){
                        int num=new JSONObject(result).getInt("result");
                        txtCollectNum.setText("取消 "+num+"人");
                        hasCollected=true;
                    }else{
                        ToastUtil.showShort(mContext,appResponse.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                ToastUtil.showShort(mContext, ThrowableUtil.getErrorMsg(t));
            }
        });
    }

    /**
     * 取消收藏店铺
     */
    private void cancelShopCollect(){
        Map<String,Object> params=new HashMap<>();
        params.put("shopId",mShop.getShopId());
        params.put("userId",mUser.getUserId());
        RequestBody body=RequestBody.create(jsonMediaType,mGson.toJson(params));
        Call<ResponseBody> call=goodsWebService.cancelShopCollect(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String result=response.body().string();
                    LogUtil.print("result",result);
                    AppResponse appResponse=mGson.fromJson(result,AppResponse.class);
                    if(appResponse.getCode()== ResponseCode.SUCCESS){
                        int num=new JSONObject(result).getInt("result");
                        txtCollectNum.setText("收藏 "+num+"人");
                        hasCollected=false;
                    }else{
                        ToastUtil.showShort(mContext,appResponse.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                ToastUtil.showShort(mContext, ThrowableUtil.getErrorMsg(t));
            }
        });
    }

    private void initUIWithValue(){
        txtShopName.setText(mShop.getShopName());
        txtCollectNum.setText("收藏 "+mShop.getShopCollectUserTotal()+"人");
        txtGoodComment.setText("好评率"+mShop.getGoodCommentPercent()+"%");
        txtGoodsNews.setText(mShop.getRecentGoodsTotal()+"");
        txtGoodsTotal.setText(mShop.getGoodsTotal()+"");
        ImageLoader.getInstance().displayImage(Constants.PICTURE_URL+mShop.getShopIcon(),imgShop, DisplayImageOptionsUtil.getOptions());
        ImageLoader.getInstance().displayImage(Constants.PICTURE_URL+mShop.getHeadImage(),imgHead, DisplayImageOptionsUtil.getOptions());
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_shop_goods;
    }

    @Override
    protected void initView() {
        StatusBarCompat.setStatusBarColor(this, Color.TRANSPARENT,false);
        imgShop= (ImageView) findViewById(R.id.img_shop);
        imgHead=(ImageView) findViewById(R.id.img_head);
        txtShopName= (TextView) findViewById(R.id.txt_shop_name);
        txtGoodComment= (TextView) findViewById(R.id.txt_comment_num);
        txtCollectNum= (TextView) findViewById(R.id.txt_shop_collect_num);
        txtGoodsTotal= (TextView) findViewById(R.id.txt_goods_total);
        txtGoodsNews= (TextView) findViewById(R.id.txt_goods_new_num);
        gvGoods= (LRecyclerView) findViewById(R.id.rv_goods);
        viewHome=findViewById(R.id.view_home);
        viewAll=findViewById(R.id.view_all);
        viewNew=findViewById(R.id.view_new);
        btnHome= (Button) findViewById(R.id.btn_home);
        txtAllTitle= (TextView) findViewById(R.id.txt_goods_total_title);
        txtAllNum= (TextView) findViewById(R.id.txt_goods_total);
        txtNewTitle= (TextView) findViewById(R.id.txt_goods_new_num_title);
        txtNewNum= (TextView) findViewById(R.id.txt_goods_new_num);
        llFilter=(LinearLayout)findViewById(R.id.ll_filter);
        txtMult= (TextView) findViewById(R.id.txt_mult);
        txtSell= (TextView) findViewById(R.id.txt_sell);
        txtTime= (TextView) findViewById(R.id.txt_time);
        txtPrice= (TextView) findViewById(R.id.txt_price);
        etSearch= (EditText) findViewById(R.id.et_words);
        gvGoods.setHasFixedSize(true);
        gvGoods.setLayoutManager(new GridLayoutManager(this,2));
        gvGoods.setHeaderViewColor(R.color.colorAccent, R.color.dark ,android.R.color.white);
        gvGoods.setFooterViewColor(R.color.colorAccent, R.color.dark ,android.R.color.white);
        gvGoods.setFooterViewHint("拼命加载中","已经全部为你呈现了","网络不给力啊，点击再试一次吧");


    }
}
