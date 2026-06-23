package fmedia;

public class MediaData {
    public final String title;
    public final String artist;
    public final String artUrl;
    public final long durationMicros;
    public final long positionMicros;
    public final boolean playing;
    public final String playerName;

    public MediaData(String title, String artist, String artUrl, long durationMicros, long positionMicros, boolean playing, String playerName) {
        this.title = title;
        this.artist = artist;
        this.artUrl = artUrl;
        this.durationMicros = durationMicros;
        this.positionMicros = positionMicros;
        this.playing = playing;
        this.playerName = playerName;
    }

    public String getTitle() { return title != null ? title : ""; }
    public String getArtist() { return artist != null ? artist : ""; }

    @Override
    public String toString() {
        return (playing ? ">" : "||") + " " + title + " - " + artist + " [" + playerName + "]";
    }
}
