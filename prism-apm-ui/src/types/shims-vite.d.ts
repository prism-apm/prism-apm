import 'vite';
import { MinifyOptions } from 'terser';

declare module 'vite' {
    // noinspection JSUnusedGlobalSymbols
    interface TerserOptions extends MinifyOptions {
        maxWorkers?: number;
    }
}