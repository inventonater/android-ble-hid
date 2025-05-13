using System;
using UnityEngine;

namespace Inventonater
{
    public class SingleAxisMappingVolumeIncremental : SingleAxisMappingThrottled
    {
        private MappableActionRegistry _registry;
        private readonly Action _increment = delegate { };
        private readonly Action _decrement = delegate { };
        private int _pendingDelta;

        public SingleAxisMappingVolumeIncremental(Axis axis, MappableActionRegistry registry, float scale = DefaultScale, float timeInterval = DefaultTimeInterval)
            : base(axis, scale, timeInterval)
        {
            _registry = registry;
            if (registry.TryGetMappedAction(MappableActionId.VolumeUp, out var increment)) _increment = increment.Invoke;
            if (registry.TryGetMappedAction(MappableActionId.VolumeDown, out var decrement)) _decrement = decrement.Invoke;
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
