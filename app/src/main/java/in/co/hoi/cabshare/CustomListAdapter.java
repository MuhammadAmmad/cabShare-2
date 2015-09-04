package in.co.hoi.cabshare;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Ujjwal on 01-06-2015.
 */
public class CustomListAdapter extends BaseAdapter implements View.OnClickListener {

    /*********** Declare Used Variables *********/
    private Activity activity;
    private List<SavedPlaceItem> data;
    private static LayoutInflater inflater=null;
    public Resources res;
    SavedPlaceItem tempValues=null;
    int i=0;

    /*************  CustomAdapter Constructor *****************/
    public CustomListAdapter(Activity a, List<SavedPlaceItem> d,Resources resLocal) {

        /********** Take passed values **********/
        activity = a;
        data=d;
        res = resLocal;

        /***********  Layout inflator to call external xml layout () ***********/
        inflater = ( LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        if(data.size()<=0)
            return 1;
        return data.size();
    }

    @Override
    public SavedPlaceItem getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /********* Create a holder Class to contain inflated xml file elements *********/
    public static class ViewHolder{

        public TextView text1;
        public TextView text2;
        public de.hdodenhof.circleimageview.CircleImageView locTypeImg;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v  = convertView;
        ViewHolder holder;

        if(convertView==null){

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            v = inflater.inflate(R.layout.listitem, null);

            /****** View Holder Object to contain tabitem.xml file elements ******/

            holder = new ViewHolder();
            holder.locTypeImg = (de.hdodenhof.circleimageview.CircleImageView) v.findViewById(R.id.loc_type);
            holder.text1=(TextView)v.findViewById(R.id.address1);
            holder.text2 = (TextView) v.findViewById(R.id.address2);

            /************  Set holder with LayoutInflater ************/
            v.setTag( holder );
        }
        else
            holder=(ViewHolder)v.getTag();

        if(data.size()>0)
        {
            /***** Get each Model object from Arraylist ********/
            tempValues=null;
            tempValues = ( SavedPlaceItem ) data.get( position );

            /************  Set Model values in Holder elements ***********/

            if(tempValues.getType().equals("Home"))
            {
                holder.locTypeImg.setImageResource(R.drawable.ic_home);
            }
            else if(tempValues.getType().equals("Office")){
                holder.locTypeImg.setImageResource(R.drawable.ic_business_black_24dp);
            }
            else if(tempValues.getType().equals("Current")){
                holder.locTypeImg.setImageResource(R.drawable.ic_my_location_black_24dp);
            }
            else if(tempValues.getType().equals("Favorite1")){
                holder.locTypeImg.setImageResource(R.drawable.ic_star_border_black_24dp);
            }
            else if(tempValues.getType().equals("Favorite2")){
                holder.locTypeImg.setImageResource(R.drawable.ic_star_border_black_24dp);
            }
            else if(tempValues.getType().equals("Favorite3")){
                holder.locTypeImg.setImageResource(R.drawable.ic_star_border_black_24dp);
            }
            else if(tempValues.getType().equals("Airport")){
                holder.locTypeImg.setImageResource(R.drawable.ic_local_airport_black_24dp);
            }
            else if(tempValues.getType().equals("Railway")){
                holder.locTypeImg.setImageResource(R.drawable.ic_directions_railway_black_24dp);
            }
            holder.text1.setText( tempValues.getAddress1() );
            holder.text2.setText( tempValues.getAddress2() );

            v.setOnClickListener(new OnItemClickListener( position ));
        }
        return v;
    }

    @Override
    public void onClick(View v) {
        Log.v("CustomAdapter", "=====Row button clicked=====");
    }

    private class OnItemClickListener  implements View.OnClickListener {
        private int mPosition;

        OnItemClickListener(int position){
            mPosition = position;
        }

        @Override
        public void onClick(View arg0) {


            SearchActivity sct = (SearchActivity) activity;

            /****  Call  onItemClick Method inside CustomListViewAndroidExample Class ( See Below )****/

            sct.onItemClick(mPosition);
        }
    }
}
