/*
 * Created on May 7, 2004
 *
 */
package com.dotmarketing.portlets.files.model;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.farng.mp3.AbstractMP3Tag;
import org.farng.mp3.TagException;

import com.dotmarketing.business.APILocator;

/**
 * @author rocco
 * 
 */
public class MP3File extends File {

	/**
	 * 
	 */

	String[] genres = new String[] { "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop",
			"Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno",
			"Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient",
			"Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game",
			"Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative",
			"Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic",
			"Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap",
			"Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes",
			"Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll",
			"Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival",
			"Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock",
			"Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech",
			"Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire",
			"Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle",
			"Duet", "Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall" };

	private static final long serialVersionUID = 1L;

	String title;

	String artist;

	String album;

	String year;

	int bitrate;

	double frequency;

	long duration;

	String genre;

	public MP3File(File file) throws IOException, IllegalAccessException, InvocationTargetException, TagException {

		if (!file.getFileName().toLowerCase().endsWith("mp3")) {
			throw new IOException("Not an mp3 file");
		}
		BeanUtils.copyProperties(this, file);

		java.io.File ioFile = APILocator.getFileAPI().getAssetIOFile(file);
		org.farng.mp3.MP3File mp3 = new org.farng.mp3.MP3File(ioFile);

		if (ioFile.length() > 0 && mp3.getBitRate() > 0) {
			duration = (long) ((double) ioFile.length() / ((double) mp3.getBitRate() / 8.0));
		}
		bitrate = mp3.getBitRate();
		frequency = mp3.getFrequency();
		// get the id3 tags
		AbstractMP3Tag tag = null;
		if (mp3.hasID3v1Tag()) {
			tag = mp3.getID3v1Tag();

			title = tag.getSongTitle();
			if(!"New Artist".equals( tag.getLeadArtist())){
				artist = tag.getLeadArtist();
			}
			if(!"New Title".equals( tag.getAlbumTitle())){
				album = tag.getAlbumTitle();
			}
			year = tag.getYearReleased();
			genre = lookupGenre(tag.getSongGenre());

		}
		if (mp3.hasID3v2Tag()) {
			tag = mp3.getID3v2Tag();
			if (title == null)
				title = tag.getSongTitle();
			if (artist == null && !"New Artist".equals( tag.getLeadArtist()))
				artist = tag.getLeadArtist();
			if (album == null && !"New Title".equals( tag.getAlbumTitle()))
				album = tag.getAlbumTitle();
			if (year == null)
				year = tag.getYearReleased();
			if (genre == null)
				genre = lookupGenre(tag.getSongGenre());

		}
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public int getBitrate() {
		return bitrate;
	}

	public void setBitrate(int bitrate) {
		this.bitrate = bitrate;
	}

	public double getFrequency() {
		return frequency;
	}

	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	private String lookupGenre(String x) {

		try {
			x = x.replaceAll("[()]", "");
			int y = Integer.parseInt(x);
			return genres[y];
		} catch (Exception e) {
			return null;
		}
	}

}
