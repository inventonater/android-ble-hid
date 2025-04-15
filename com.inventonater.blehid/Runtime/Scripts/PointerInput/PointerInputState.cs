namespace Inventonater.BleHid
{
    /// <summary>
    /// Different input states for any pointer device
    /// </summary>
    public enum PointerInputState
    {
        None,
        Begin,   // Input started (mouse down, touch began)
        Move,    // Input moved (mouse moved, touch moved)
        End      // Input ended (mouse up, touch ended/canceled)
    }
}