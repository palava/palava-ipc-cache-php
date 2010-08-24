<?php

/**
 * Caches command results and immidiatly returns them
 * if remote command is @Cached
 */
class IpcCache extends AbstractPalavaModule {

    const NAME = __CLASS__;

    const PKEY_CACHED = "CACHED";

    const CONFIG_ENABLED = "cache.enabled";
    const CONFIG_COMMANDS = "cache.commands";
    const DEFAULT_ENABLED = true;

    public static $DEFAULT_COMMANDS = array("de.cosmocode.palava.util.ValuesOf");

    private $cache = array();

    private $oneShot = false;

    private function enabled() {
        return $this->get(IpcCache::CONFIG_ENABLED, IpcCache::DEFAULT_ENABLED);
    }

    private function callKey(&$call) {
        $key = array(
            $call[Palava::PKEY_SESSION],
            $call[Palava::PKEY_COMMAND],
            $call[Palava::PKEY_ARGUMENTS]
        );
        return json_encode($key);
    }

    public function oneShot() {
        $this->oneShot = true;
    }

    private function isConfigCached(&$call) {
        $commands = $this->get(IpcCache::CONFIG_COMMANDS, IpcCache::$DEFAULT_COMMANDS);
        $command = $call[Palava::PKEY_COMMAND];

        if (in_array($command, $commands)) {
            return $command;
        } else {
            return null;
        }
    }

    public function preCall(&$call) {
        // module disabled?
        if (!$this->enabled()) {
            return null;
        }

        // cache disabled for one call?
        if ($this->oneShot) {
            $this->oneShot = false;
            return null;
        }

        // cached by config?
        if (($key = $this->isConfigCached($call)) === null) {
            $key = $this->callKey($call);
        }

        // return the cached result if available        
        if (isset($this->cache[$key])) {
            return $this->cache[$key];
        } else {
            return null;
        }
    }

    public function postCall(&$call, &$result) {
        // module disabled?
        if (!$this->enabled()) {
            return;
        }

        if (isset($result[Palava::PKEY_EXCEPTION])) {
            // do not cache exceptions
            return;
        }

        if (($key = $this->isConfigCached($call)) === null) {
            // get meta informations
            if (!isset($result[IpcCache::PKEY_CACHED])) {
                throw new Exception(IpcCache::PKEY_CACHED." not set; PhpCacheModule installed?");
            }

            if ($result[IpcCache::PKEY_CACHED]) {
                $key = $this->callKey($call);
            }
        }

        // if meta CACHED is true, cache it
        if (!is_null($key)) {
            $this->cache[$key] = $result;
        }
    }

}
