package com.github.uchan_nos.c_helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.osgi.framework.BundleContext;

import com.github.uchan_nos.c_helper.util.Util;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.github.uchan_nos.c_helper"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    // デバッグモードフラグ
    private boolean debug = false;

    // 現在表示中のマーカー一覧
    private Collection<IMarker> showingMarkers = new ArrayList<IMarker>();

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        // -debugオプションに対応する
        Hashtable<String, String> properties = new Hashtable<String, String>(4);
        properties.put(DebugOptions.LISTENER_SYMBOLICNAME, PLUGIN_ID);

        context.registerService(
                DebugOptionsListener.class,
                new DebugOptionsListener() {
                    @Override
                    public void optionsChanged(DebugOptions options) {
                        Activator.this.debug = options.getBooleanOption(PLUGIN_ID + "/debug", false);

                        // このプラグインのデバッグフラグが明示的に指定されていたら、
                        // ロガーを詳細レベルに設定する
                        Logger logger = Activator.getLogger();
                        Level level = Activator.this.debug ? Level.ALL : Level.INFO;
                        logger.setLevel(level);
                        Util.GetConsoleHandler(logger).setLevel(level);
                    }
                },
                properties);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * プラグインのデフォルトロガーを返す.
     */
    public static Logger getLogger() {
        return Logger.getLogger(PLUGIN_ID);
    }

    /**
     * デバッグモードかどうかを返す.
     */
    public boolean isDebugMode() {
        return debug;
    }

    /**
     * 現在表示中のマーカー一覧を返す.
     */
    public Collection<IMarker> getShowingMarkers() {
        return showingMarkers;
    }
}
