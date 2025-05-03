using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class SingleAxisMappingIncremental : SingleAxisMappingThrottled
    {
        private readonly Action _increment;
        private readonly Action _decrement;
        private int _pendingDelta;

        public SingleAxisMappingIncremental(Axis axis, Action increment, Action decrement, float scale = DefaultScale, float timeInterval = DefaultTimeInterval)
            : base(axis, scale, timeInterval)
        {
            _increment = increment;
            _decrement = decrement;
        }

        protected override void ProcessReset() => _pendingDelta = 0;

        protected override void AddSingleAxisPendingDelta(int pendingDelta) => _pendingDelta = pendingDelta;

        protected override void ThrottledUpdate()
        {
            if (_pendingDelta > 0)
            {
                _pendingDelta--;
                _increment();
            }

            if (_pendingDelta < 0)
            {
                _pendingDelta++;
                _decrement();
            }
        }
    }
}
