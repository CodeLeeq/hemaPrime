package com.streamhemaprime.hemaprime.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.streamhemaprime.hemaprime.R;
import com.streamhemaprime.hemaprime.model.Card;
import com.streamhemaprime.hemaprime.network.APIClient;
import com.streamhemaprime.hemaprime.network.APIInterface;
import com.streamhemaprime.hemaprime.util.NetworkUtils;
import com.streamhemaprime.hemaprime.ui.adapter.CardsAdapter;
import com.streamhemaprime.hemaprime.util.ParserUtils;
import com.streamhemaprime.hemaprime.util.UiUtils;
import com.streamhemaprime.hemaprime.util.sharedpref.PrefKeys;
import com.streamhemaprime.hemaprime.util.sharedpref.PrefUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.streamhemaprime.hemaprime.network.APIConstants.Constants;
import static com.streamhemaprime.hemaprime.network.APIConstants.Params;

public class PaymentsActivity extends BaseActivity implements CardsAdapter.CardListener {

    private static final int GET_ADDED_CARD = 100;
    @BindView(R.id.addCard)
    View addCard;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.cardsRecycler)
    RecyclerView cardsRecycler;
    @BindView(R.id.noResultLayout)
    View noResultLayout;

    CardsAdapter cardsAdapter;
    ArrayList<Card> cards = new ArrayList<>();
    APIInterface apiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);
        ButterKnife.bind(this);
        apiInterface = APIClient.getClient().create(APIInterface.class);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setUpCards();
    }

    private void setUpCards() {
        cardsRecycler.setHasFixedSize(true);
        cardsRecycler.setLayoutManager(new LinearLayoutManager(this));
        cardsAdapter = new CardsAdapter(this, cards, this);
        cardsRecycler.setItemAnimator(new DefaultItemAnimator());
        cardsRecycler.setAdapter(cardsAdapter);
        getCards();
    }

    private void getCards() {
        UiUtils.showLoadingDialog(this);
        PrefUtils preferences = PrefUtils.getInstance(this);
        Call<String> call = apiInterface.getCards(preferences.getIntValue(PrefKeys.USER_ID, -1)
                , preferences.getStringValue(PrefKeys.SESSION_TOKEN, "")
                , preferences.getIntValue(PrefKeys.ACTIVE_SUB_PROFILE, -1));
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                cards.clear();
                UiUtils.hideLoadingDialog();
                JSONObject cardsResponse = null;
                try {
                    cardsResponse = new JSONObject(response.body());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (cardsResponse != null)
                    if (cardsResponse.optString(Params.SUCCESS).equals(Constants.TRUE)) {
                        JSONArray cardsArray = cardsResponse.optJSONArray(Params.DATA);
                        for (int i = 0; i < cardsArray.length(); i++) {
                            try {
                                JSONObject cardObj = cardsArray.getJSONObject(i);
                                Card card = ParserUtils.parseCardData(cardObj);
                                cards.add(card);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        notifyDataChange();
                    } else {
                        UiUtils.showShortToast(PaymentsActivity.this, cardsResponse.optString(Params.ERROR_MESSAGE));
                    }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                UiUtils.hideLoadingDialog();
                NetworkUtils.onApiError(PaymentsActivity.this);
            }
        });
    }

    private void notifyDataChange() {
        cardsAdapter.notifyDataSetChanged();
        boolean isEmpty = cards.size() == 0;
        noResultLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cardsRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @OnClick(R.id.addCard)
    public void onViewClicked() {
        Intent i = new Intent(this, AddCardActivity.class);
        startActivityForResult(i, GET_ADDED_CARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == GET_ADDED_CARD && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                JSONObject addedCard;
                try {
                    addedCard = new JSONObject(data.getStringExtra(AddCardActivity.ADDED_CARD));
                    Card card = ParserUtils.parseCardData(addedCard);
                    cards.add(card);
                    notifyDataChange();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCardDeleted(boolean isEmpty, int position) {
        getCards();
    }
}
