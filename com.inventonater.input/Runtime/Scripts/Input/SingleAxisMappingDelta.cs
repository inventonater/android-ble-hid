using System;
using UnityEngine;

namespace Inventonater
{
    public class SingleAxisMappingDelta : SingleAxisMappingThrottled
    {
        private readonly Action<int> _setDeltaAction;
        private int _pendingDelta;

        public SingleAxisMappingDelta(Axis axis, Action<int> setDeltaAction, float scale = DefaultScale, float timeInterval = DefaultTimeInterval)
            : base(axis, scale, timeInterval)
        {
            _setDeltaAction = setDeltaAction;
        }

        protected override void ProcessReset()
        {
            _pendingDelta = 0;
        }

        protected override void AddSingleAxisPendingDelta(int pendingDelta) => _pendingDelta += pendingDelta;

        protected override void ThrottledUpdate()
        {
            if (_pendingDelta == 0) return;

            Debug.Log(_pendingDelta);
            _setDeltaAction(_pendingDelta);
            _pendingDelta = 0;
        }
    }
}
