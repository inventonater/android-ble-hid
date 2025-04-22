namespace Inventonater.BleHid
{
    public static class ExecutionOrder
    {
        public const int Initialize = -100;
        public const int Polling = -85;
        public const int Preprocess = -20;
        public const int Process = 20;
        public const int PostProcess = 40;
    }
}
