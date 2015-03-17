package org.draijer.nvkf.helper;

import java.util.ArrayList;

import org.draijer.nvkf.R;
import org.draijer.nvkf.model.News;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NewsAdapter extends BaseAdapter {
	ArrayList<News> mNews;
	LayoutInflater mInflater;
	
	public NewsAdapter(Context context, ArrayList<News> news){
		mNews = new ArrayList<News>();
		mNews.addAll(news);
		notifyDataSetChanged();
        mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return mNews.size();
	}

	@Override
	public News getItem(int position) {
		return mNews.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {		
		ViewHolder holder;
		
		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.listitem, null);
			
			holder = new ViewHolder();
			holder.mTitle = (TextView) convertView.findViewById(R.id.tvTitel);
			holder.mDescr = (TextView) convertView.findViewById(R.id.tvItem);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		News news = getItem(position);
		
		//TextView mTitle = (TextView) convertView.findViewById(R.id.tvTitel);
		//mTitle.setText(news.getTitle());
		holder.mTitle.setText(news.getTitle());
		
		//TextView mDescr = (TextView) convertView.findViewById(R.id.tvItem);
		//mDescr.setText(news.getDescription());
		holder.mDescr.setText(news.getDescription());
				
		return convertView;
	}
	
    private class ViewHolder {
        TextView mTitle;
        TextView mDescr;
    }
}
