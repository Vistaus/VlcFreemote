package com.nico.vlcfreemote.vlc_connector;

import com.nico.vlcfreemote.vlc_connector.http_utils.Wget;
import com.nico.vlcfreemote.vlc_connector.http_utils.XmlMogrifier;
import com.nico.vlcfreemote.vlc_connector.http_utils.XmlObjectReader;

import java.util.List;

public abstract class VlcCommand_ReturnsVlcStatus implements VlcCommand {

    private final VlcStatus.Observer cb;

    VlcCommand_ReturnsVlcStatus(VlcStatus.ObserverRegister register) {
        this.cb = register.getVlcStatusObserver();
    }

    @Override
    public Wget.Callback getWgetCallback(final VlcCommand.GeneralCallback generalCallback) {
        return new Wget.Callback() {
            @Override
            public void onConnectionError(Exception request_exception) { generalCallback.onConnectionError(); }

            @Override
            public void onAuthFailure() {
                generalCallback.onAuthError();
            }

            @Override
            public void onHttpFail(int httpRetCode, String result) {
                cb.onVlcStatusFetchError();
            }

            @Override
            public void onResponse(String result) {
                if (cb == null) {
                    // If there's no one to observe Vlc's status, don't bother parsing it
                    return;
                }

                final XmlMogrifier.XmlKeyValReader<VlcStatus> kvReader;
                kvReader = new XmlMogrifier.XmlKeyValReader<VlcStatus>(VlcStatus.class) {
                    @Override
                    protected void parseValue(VlcStatus obj, String key, String val) {
                        switch (key) {
                            case "length":
                                obj.length = Integer.parseInt(val);
                                break;
                            case "position":
                                obj.position = VlcStatus.normalizePosFromVlcTo0_100( Float.parseFloat(val) );
                                break;
                            case "volume":
                                obj.volume = VlcStatus.normalizeVolumeFromVlcTo0_100( Integer.parseInt(val) );
                                break;
                            case "time":
                                obj.time = Integer.parseInt(val);
                                break;
                            case "rate":
                                obj.rate = Float.parseFloat(val);
                                break;
                            case "audiodelay":
                                obj.audiodelay = Float.parseFloat(val);
                                break;
                            case "subtitledelay":
                                obj.subtitledelay = Float.parseFloat(val);
                                break;
                            case "repeat":
                                obj.repeat = Boolean.parseBoolean(val);
                                break;
                            case "loop":
                                obj.loop = Boolean.parseBoolean(val);
                                break;
                            case "random":
                                obj.random = Boolean.parseBoolean(val);
                                break;
                            case "fullscreen":
                                obj.fullscreen = Boolean.parseBoolean(val);
                                break;
                            case "state":
                                obj.state = val;
                                break;
                            case "filename":
                                obj.currentMedia_filename = val;
                                break;
                            case "album":
                                obj.currentMedia_album = val;
                                break;
                            case "title":
                                obj.currentMedia_title = val;
                                break;
                            case "artist":
                                obj.currentMedia_artist = val;
                                break;
                            case "track_number":
                                obj.currentMedia_trackNumber = Integer.parseInt(val);
                                break;
                            case "track_total":
                                obj.currentMedia_tracksTotal = Integer.parseInt(val);
                                break;
                            default:
                            /* Do nothing, we don't care about this tag */
                        }
                    }
                };

                new XmlObjectReader<>(result, null, kvReader, new XmlMogrifier.Callback<VlcStatus>() {
                    @Override
                    public void onXmlSystemError(Exception e) {
                        generalCallback.onSystemError(e);
                    }

                    @Override
                    public void onXmlDecodingError(Exception e) { cb.onVlcStatusFetchError(); }

                    @Override
                    public void onResult(List<VlcStatus> results) {
                        if (results.size() == 1) {
                            cb.onVlcStatusUpdate(results.get(0));
                        } else {
                            cb.onVlcStatusFetchError();
                        }
                    }
                });
            }
        };
    }
}