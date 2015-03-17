package org.draijer.nvkf.helper;

import java.util.ArrayList;

import org.draijer.nvkf.R;
import org.draijer.nvkf.model.Job;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class JobAdapter extends BaseAdapter {

	ArrayList<Job> mJobs;
	LayoutInflater mInflater;
	
	public JobAdapter(Context context, ArrayList<Job> jobs){
		mJobs = new ArrayList<Job>();
		mJobs.addAll(jobs);
		notifyDataSetChanged();
        mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return mJobs.size();
	}

	@Override
	public Job getItem(int position) {
		return mJobs.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {		
		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.listitem, null);
		}
		
		Job job = getItem(position);
		
		TextView mTitle = (TextView) convertView.findViewById(R.id.tvTitel);
		mTitle.setText(job.getTitle());
		
		TextView mDescr = (TextView) convertView.findViewById(R.id.tvItem);
		mDescr.setText(job.getDescription());
		
		return convertView;
	}
}
