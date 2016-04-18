package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.contacts.ContactsPermission;
import com.voipgrid.vialer.util.IconHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter to display the call records
 */
public class CallRecordAdapter extends BaseAdapter {

    private List<CallRecord> mCallRecords;

    private Context mContext;

    /**
     * Construct a new CallRecordAdapter
     * @param context
     * @param callRecords
     */
    public CallRecordAdapter(Context context, List<CallRecord> callRecords) {
        mContext = context;
        mCallRecords = callRecords;
    }

    /**
     * Set call records to the adapter
     * @param callRecords
     */
    public void setCallRecords(List<CallRecord> callRecords) {
        mCallRecords = callRecords;
        notifyDataSetChanged();
    }

    /**
     * Add call records to the adapter
     * @param callRecords
     */
    public void addCallRecords(List<CallRecord> callRecords) {
        mCallRecords.addAll(callRecords);
        notifyDataSetChanged();
    }

    /**
     * getContactNameForNumber return the name of the contact that matches the number or null.
     * @param number The number to find a contact for.
     * @return The name or null.
     */
    private String getContactNameForNumber(String number) {
        String name = null;
        // Uri for getting contact info.
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(ContactsContract.Directory.DEFAULT))
                .build();
        // Query selection.
        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? " +
                "OR " + ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + " = ?";

        // Query the database.
        Cursor contactCursor = mContext.getContentResolver().query(
                uri,
                new String[] {
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                },
                selection,
                new String[] {
                        number,
                        number
                },
                null
        );

        // Check if cursor not null.
        if (contactCursor != null) {
            // Check if we have a item.
            if (contactCursor.moveToFirst()) {
                // Get the name of the first match.
                name = contactCursor.getString(0);  // display name primary
            }
            // Always close cursor.
            contactCursor.close();
        }
        return name;
    }


    @Override
    public int getCount() {
        return mCallRecords.size();
    }

    @Override
    public CallRecord getItem(int position) {
        return mCallRecords.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        // Get the call record.
        CallRecord callRecord = getItem(position);
        String name = null;
        String number = "";
        // Default resource for direction.
        int resource = 0;

        if(callRecord != null) {
            // Get the direction from the call record.
            String direction = callRecord.getDirection();

            // Set the drawable resource.
            if (direction.equals(CallRecord.DIRECTION_OUTBOUND)) {
                number = callRecord.getDialedNumber();
                resource = R.drawable.ic_outgoing;
            } else if (direction.equals(CallRecord.DIRECTION_INBOUND)) {
                number = callRecord.getCaller();
                if (callRecord.getDuration() == 0) {
                    resource = R.drawable.ic_incoming_missed;
                } else {
                    resource = R.drawable.ic_incoming;
                }
            }

            // Get possible name or null.
            if (ContactsPermission.hasPermission(mContext)) {
                name = getContactNameForNumber(number);
            }
        }

        if(convertView == null) {
            // Inflate the layout.
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.list_item_call_record, parent, false);
        }

        String firstLetterOfName = name != null ? name.substring(0, 1) : "";

        Bitmap bitmapImage = IconHelper.getCallerIconBitmap(firstLetterOfName, Color.BLUE);

        View photoView = convertView.findViewById(R.id.text_view_contact_icon);

        ((CircleImageView) photoView).setImageBitmap(bitmapImage);
        // Set up the ViewHolder.
        viewHolder = new ViewHolder();
        viewHolder.title = (TextView) convertView.findViewById(R.id.text_view_contact_name);
        viewHolder.information = (TextView) convertView.findViewById(
                R.id.text_view_contact_information
        );

        // Store the holder with the view.
        convertView.setTag(viewHolder);

        if(callRecord != null) {
            // Set name or number as text.
            if(number != null && PhoneNumberUtils.isAnonymousNumber(callRecord.getCaller())) {
                viewHolder.title.setText(convertView.getContext().getString(R.string.supressed_number));
            } else if (name != null) {
                viewHolder.title.setText(name);
            } else {
                viewHolder.title.setText(number);
            }

            // Set the compound drawable to the view.
            viewHolder.information.setCompoundDrawablesWithIntrinsicBounds(resource, 0, 0, 0);

            // Format the date.
            SimpleDateFormat dateFormat = new SimpleDateFormat(CallRecord.DATE_FORMAT);
            Date date = null;
            try {
                date = dateFormat.parse(callRecord.getCallDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // Set the call record date information to the view.
            viewHolder.information.setText(DateUtils.getRelativeDateTimeString(
                    mContext, date.getTime(), DateUtils.SECOND_IN_MILLIS,
                    DateUtils.YEAR_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME));
        }

        return convertView;
    }

    static class ViewHolder {
        TextView title;
        TextView information;
    }
}
