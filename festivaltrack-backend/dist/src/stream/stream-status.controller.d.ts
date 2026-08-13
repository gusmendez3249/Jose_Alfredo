export declare class StreamStatusController {
    private streamUrl;
    private isLive;
    getStatus(): {
        streamUrl: string;
        emulatorUrl: string;
        isLive: boolean;
        port: number;
    };
    setStatus(body: {
        streamUrl: string;
        isLive: boolean;
    }): {
        ok: boolean;
        streamUrl: string;
        isLive: boolean;
    };
}
