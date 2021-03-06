package jpro.smarttrains;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

import SmartTrainTools.RailwayCodes;
import SmartTrainTools.Train;
import SmartTrainsSQL.DatabaseHandler;
import SmartTrainsSQL.TrainBean;

public class TrainsByNo extends AppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent in=new Intent(getApplicationContext(),MainHome.class);
        startActivity(in);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db=new DatabaseHandler(this);
        setContentView(R.layout.activity_trains_by_no);
        trainBeanArrayList=new ArrayList<>();
        clearimg=(ImageView)findViewById(R.id.clearAllImg);
        recents=(LinearLayout)findViewById(R.id.recents_trains_by_no_LL);
        trains=new ArrayList<Train>(Globals.rc.getTrains());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        System.out.println("TRAINS_AR_SIZE:"+trains.get(0));

        setTitle("Trains by Name or Number");
        pBar=(ProgressBar)findViewById(R.id.pbar);
        trBox=(DelayedAutoCompleteTextView) findViewById(R.id.trNameBox);

        trBox.setThreshold(4);
        CustomTrainSpinnerAdapter ad=new CustomTrainSpinnerAdapter(this,R.layout.activity_trains_by_no,R.id.txtContent,trains);
        trBox.setAdapter(ad);
        ProgressBar p=(ProgressBar)findViewById(R.id.pb_loading_indicator);
        trBox.setLoadingIndicator(p);
        pBar.setVisibility(View.GONE);


        deletethis=(ImageView)findViewById(R.id.delete_this);
        /*deletethis.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout vwParentRow = (LinearLayout)view.getParent();
                TextView child = (TextView)vwParentRow.getChildAt(0);
                TrainBean tb=new TrainBean();
                tb.setTrno(child.getText().toString());
                db.deleteTrainSearch(tb);
                trainBeanArrayList.clear();
                trainBeanArrayList.addAll(db.getAllTrainSearches());
                recentTrainSearchesListAdapter.update(trainBeanArrayList);
            }
        });
*/

        recentsLV=(ListView)findViewById(R.id.recents_listview);
        trainBeanArrayList.addAll(db.getAllTrainSearches());
        recentTrainSearchesListAdapter=new RecentTrainSearchesListAdapter(getApplicationContext(),trainBeanArrayList);
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
                if(trainBeanArrayList.size()!=0) {
                    new AlertDialog.Builder(TrainsByNo.this).setTitle("Clear History?").setMessage("Clear all recent Train searches?").setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            db.deleteAllTrainSearches();
                            trainBeanArrayList.clear();
                            recentTrainSearchesListAdapter.update(trainBeanArrayList);
                        }
                    }).setNegativeButton("NO",null).show();
                }

            }
        });
        recentsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TrainBean T=(TrainBean) recentTrainSearchesListAdapter.getItem(i);
                try{
                    lastTrainSelected=new Train(T.getTrno(),false,false);
                    lastTrainSelected.setName(T.getTrname());
                    new GetTrainInfo().execute(lastTrainSelected);
                } catch (IOException E){

                }

            }
        });


    }

    private void retry(){
        new GetTrainInfo().execute(lastTrainSelected);

    }

    private class GetTrainInfo extends AsyncTask<Train,Void,Void>{

        @Override
        protected void onPreExecute() {
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
                    //TrainBean trainBean=new TrainBean(t.getSource().getCode(),t.getDestination().getCode(),t.getName(),t.getNo());

                    db.addTrain(t);
                    trainBeanArrayList.clear();
                    trainBeanArrayList.addAll(db.getAllTrainSearches());
                    recentTrainSearchesListAdapter.update(trainBeanArrayList);
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
    DatabaseHandler db;
    RecentTrainSearchesListAdapter recentTrainSearchesListAdapter;
    LinearLayout recents;
    Train lastTrainSelected=null;
    ListView recentsLV;
    ImageView deletethis;
    ImageView clearimg;
    ArrayList<Train> trains;
    ArrayList<TrainBean> trainBeanArrayList;
    Train T;
}
