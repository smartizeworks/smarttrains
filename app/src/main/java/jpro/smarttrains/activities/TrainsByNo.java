package jpro.smarttrains.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.io.IOException;
import java.util.ArrayList;

import SmartTrainTools.Train;
import SmartTrainsDB.modals.Modal;
import SmartTrainsDB.modals.RecentTrain;
import Utilities.SmartAnimator;
import commons.Config;
import jpro.smarttrains.R;
import jpro.smarttrains.adapters.CustomTrainSpinnerAdapter;
import jpro.smarttrains.adapters.RecentTrainSearchesListAdapter;
import jpro.smarttrains.views.DelayedAutoCompleteTextView;

public class TrainsByNo extends AppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trains_by_no);
        clearimg=(ImageView)findViewById(R.id.clearAllImg);
        recents=(LinearLayout)findViewById(R.id.recents_trains_by_no_LL);
        trains = new ArrayList<Train>(Config.rc.getTrains());

        clearTr = (ImageView) findViewById(R.id.trains_by_no_clear_btn);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Trains by Name or Number");
        pBar=(ProgressBar)findViewById(R.id.pbar);
        trBox=(DelayedAutoCompleteTextView) findViewById(R.id.trNameBox);
        trBox.setThreshold(3);
        CustomTrainSpinnerAdapter ad=new CustomTrainSpinnerAdapter(this,R.layout.activity_trains_by_no,R.id.txtContent,trains);
        trBox.setAdapter(ad);
        ProgressBar p=(ProgressBar)findViewById(R.id.pb_loading_indicator);
        trBox.setLoadingIndicator(p);
        pBar.setVisibility(View.GONE);
        clearTr.setAlpha(0F);

        clearTr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trBox.setText("");
            }
        });
        trBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (trBox.getText().toString().length() > 0) {
                    clearTr.setAlpha(0.4F);
                } else {
                    clearTr.setAlpha(0F);
                }
            }
        });




        recentsLV=(ListView)findViewById(R.id.recents_listview);
        recentTrainSearchesListAdapter = new RecentTrainSearchesListAdapter(TrainsByNo.this, R.layout.list_item_small_train_view, RecentTrain.objects.getAllRecentTrain());
        recentsLV.setAdapter(recentTrainSearchesListAdapter);

        trBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                trBox.setEnabled(false);
                lastTrainSelected=trains.get(i);
                new GetTrainInfo().execute(lastTrainSelected);
            }
        });

        clearimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recentTrainSearchesListAdapter.isEmpty()) {
                    new AlertDialog.Builder(TrainsByNo.this).setTitle("Clear History?").setMessage("Clear all recent Train searches?").setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            RecentTrain.objects.deleteAllRecentTrains();
                            recentTrainSearchesListAdapter.update(new ArrayList<Modal>());
                        }
                    }).setNegativeButton("NO",null).show();
                }

            }
        });

        recentsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RecentTrain T = (RecentTrain) recentTrainSearchesListAdapter.getItem(i);
                try{
                    lastTrainSelected = new Train(T.get(RecentTrain.TRAIN_NO).toString(), false, false);
                    lastTrainSelected.setName(T.get(RecentTrain.TRAIN_NAME).toString());
                    new GetTrainInfo().execute(lastTrainSelected);
                } catch (IOException E){
                    E.printStackTrace();
                }

            }
        });

        initAnimations();
    }

    private void initAnimations() {
        SmartAnimator.addActivityTransition(getWindow(), SmartAnimator.Type.EXPLODE, 250);
    }

    private void retry(){
        new GetTrainInfo().execute(lastTrainSelected);

    }

    private class GetTrainInfo extends AsyncTask<Train,Void,Void>{

        @Override
        protected void onPreExecute() {
            clearTr.setAlpha(0F);
            trBox.setText(lastTrainSelected.getNo()+"-"+lastTrainSelected.getName());
            trBox.setEnabled(false);
            recents.setVisibility(View.GONE);
            pBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Train... tr) {
            t=tr[0];
            try {
                t.fetchInfo_ETrain();
            } catch (Exception e) {
                failed=true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            clearTr.setAlpha(0.4F);
            pBar.setVisibility(View.GONE);
            recents.setVisibility(View.VISIBLE);
            trBox.setEnabled(true);
                if(failed){
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_trains_by_no), "Connection Failed", Snackbar.LENGTH_INDEFINITE).setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    retry();
                                }
                            });
                    //snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                    // snackbar.setActionTextColor(Color.YELLOW);
                    snackbar.show();
                } else {
                    RecentTrain.objects.addTrain(t);
                    recentTrainSearchesListAdapter.update(RecentTrain.objects.getAllRecentTrain());
                    Intent in=new Intent(getApplicationContext(),TrainsInfo.class);
                    in.putExtra("train",t);
                    startActivity(in);
                }

        }
        boolean failed=false;
        Train t;
    }



    DelayedAutoCompleteTextView trBox;
    ProgressBar pBar;
    RecentTrainSearchesListAdapter recentTrainSearchesListAdapter;
    LinearLayout recents;
    Train lastTrainSelected=null;
    ListView recentsLV;
    ImageView deletethis;
    ImageView clearTr;
    ImageView clearimg;
    ImageButton delete_item_img;
    ArrayList<Train> trains;
    Train T;
}
