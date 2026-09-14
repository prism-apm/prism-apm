import mitt from 'mitt';

type Events = {
    'machine-change': { cause: string; value: any };
    'compare-confirm': any;
};

// Export the singleton eventBus.
export const eventBus = mitt<Events>();
