package com.epicodus.tonight.ui;

import android.app.Activity;
import android.content.ClipData;

import android.graphics.Typeface;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;

import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.epicodus.tonight.R;
import com.epicodus.tonight.adapters.ItemBaseAdapter;
import com.epicodus.tonight.adapters.ItemGridAdapter;
import com.epicodus.tonight.adapters.ItemListAdapter;
import com.epicodus.tonight.models.Business;
import com.epicodus.tonight.models.PassObject;
import com.squareup.picasso.Picasso;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

public class DrinkChooserFragment extends Fragment {
    List<Business> mDrinksArray, mSelectedBusinessesArray;
    ListView listView1;
    GridView drinkGridView;
    CardView drinkCardView;
    TextView drinkTextView;
    TextView shakeTextView;
    TextView instructionsText;
    ItemListAdapter myItemListAdapter1;
    ItemGridAdapter myItemGridAdapter3;
    LinearLayoutAbsListView area1, area3;
    private OnFirstItemDroppedInDropZoneListener mListener;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 1500;
    private long lastShakeTime = 0;
    private SensorEventListener listener;
    private MediaPlayer mediaPlayer;

    public DrinkChooserFragment() {
    }

    public static DrinkChooserFragment newInstance() {
        return new DrinkChooserFragment();
    }

    public ItemListAdapter getAdapter() {
        return myItemListAdapter1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chooser, container, false);
        listView1 = (ListView) view.findViewById(R.id.listview1);
        drinkGridView = (GridView) view.findViewById(R.id.drinkGridView);
        drinkCardView = (CardView) view.findViewById(R.id.drinkCardView);
        drinkTextView = (TextView) view.findViewById(R.id.drinkTextView);
        area1 = (LinearLayoutAbsListView) view.findViewById(R.id.pane1);
        area3 = (LinearLayoutAbsListView) view.findViewById(R.id.pane3);
        shakeTextView = (TextView) view.findViewById(R.id.shakeToShuffle);

        instructionsText = (TextView) view.findViewById(R.id.instructionsText);
        TextView shakeToShuffle = (TextView) view.findViewById(R.id.shakeToShuffle);
        Typeface journal = Typeface.createFromAsset(getActivity().getAssets(), "fonts/journal.ttf");
        instructionsText.setTypeface(journal);
        shakeToShuffle.setTypeface(journal);

        area1.setOnDragListener(myOnDragListener);
        area3.setOnDragListener(myOnDragListener);
        area1.setAbsListView(listView1);
        area3.setAbsListView(drinkGridView);

        initItems();
        myItemListAdapter1 = new ItemListAdapter(getContext(), mDrinksArray);
        myItemGridAdapter3 = new ItemGridAdapter(getContext(), mSelectedBusinessesArray);

        listView1.setAdapter(myItemListAdapter1);
        drinkGridView.setAdapter(myItemGridAdapter3);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastUpdate) > 100) {
                        long timeDifference = currentTime - lastUpdate;
                        lastUpdate = currentTime;

                        float speed = Math.abs(x + y + z - last_x - last_y - last_z)/timeDifference * 10000;
                        if (speed > SHAKE_THRESHOLD) {
                            long now = System.currentTimeMillis();
                            if (now - lastShakeTime > 1000) {
                                Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                                if (vibrator.hasVibrator()) {
                                    vibrator.vibrate(300);
                                }
                                randomizeDrink();
                            }

                            lastShakeTime = System.currentTimeMillis();
                        }
                    }

                    last_x = x;
                    last_y = y;
                    last_z = z;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(listener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        listView1.setOnItemLongClickListener(myOnItemLongClickListener);

        return view;

    }

    AdapterView.OnItemLongClickListener myOnItemLongClickListener = new AdapterView.OnItemLongClickListener(){

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                       int position, long id) {
            Business selectedItem = (Business)(parent.getItemAtPosition(position));

            ItemBaseAdapter associatedAdapter = (ItemBaseAdapter)(parent.getAdapter());
            List<Business> associatedList = associatedAdapter.getList();

            PassObject passObj = new PassObject(view, selectedItem, associatedList);

            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDrag(data, shadowBuilder, passObj, 0);

            return true;
        }

    };

    View.OnDragListener myOnDragListener = new View.OnDragListener() {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            String area;
            if(v == area1){
                area = "area1";
            }else if(v == area3){
                area = "area3";
            }else{
                area = "unknown";
            }

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    break;
                case DragEvent.ACTION_DROP:
                    PassObject passObj = (PassObject)event.getLocalState();
                    View view = passObj.getView();
                    Business passedItem = passObj.getItem();
                    List<Business> srcList = passObj.getSrcList();
                    AbsListView oldParent = (AbsListView)view.getParent();
                    ItemBaseAdapter srcAdapter = (ItemBaseAdapter)(oldParent.getAdapter());

                    LinearLayoutAbsListView newParent = (LinearLayoutAbsListView)v;
                    ItemBaseAdapter destAdapter = (ItemBaseAdapter)(newParent.absListView.getAdapter());
                    List<Business> destList = destAdapter.getList();

                    if(removeItemToList(srcList, passedItem)){
                        addItemToList(destList, passedItem);
                    }

                    if (mListener != null) {
                        mListener.onFirstItemDroppedInDropZone(passedItem);
                    }

                    Picasso.with(getContext()).load(passedItem.getImageUrl()).fit().centerCrop().into((ImageView) getView().findViewById(R.id.drinkImageView));
                    drinkTextView.setText(passedItem.getCardText());

                    drinkCardView.setVisibility(View.VISIBLE);
                    drinkGridView.setVisibility(View.GONE);

                    srcAdapter.notifyDataSetChanged();
                    destAdapter.notifyDataSetChanged();

                    mSensorManager.unregisterListener(listener);

                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                default:
                    break;
            }

            return true;
        }

    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFirstItemDroppedInDropZoneListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnItemDroppedInZoneListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void initItems(){
        mSelectedBusinessesArray = new ArrayList<>();
        Bundle bundle = getArguments();
        mDrinksArray = Parcels.unwrap(bundle.getParcelable("drinksArray"));
    }

    private boolean removeItemToList(List<Business> items, Business item){
        return items.remove(item);
    }

    private boolean addItemToList(List<Business> items, Business item){
        return items.add(item);
    }

    public interface OnFirstItemDroppedInDropZoneListener {
        void onFirstItemDroppedInDropZone(Business item);
    }

    private void randomizeDrink() {
        mDrinksArray = Business.getRandomDrink();
        myItemListAdapter1.list.clear();
        myItemListAdapter1.list.addAll(mDrinksArray);
        myItemListAdapter1.notifyDataSetChanged();
    }
}