using System;
using System.Reflection;
using UnityEngine;

namespace Inventonater
{
    public class MappableActionInfo
    {
        public MappableActionAttribute Attribute { get; }
        public MappableActionId Id => Attribute.Id;
        public string DisplayName => Attribute.DisplayName;
        public string Description => Attribute.Description;
        public Action Action { get; }
        public MethodInfo MethodInfo { get; }
        public object Target { get; }

        public MappableActionInfo(MappableActionAttribute attribute, MethodInfo methodInfo, object target)
        {
            Attribute = attribute;
            MethodInfo = methodInfo;
            Target = target;
            Action = () => methodInfo.Invoke(target, null);
        }

        public void Invoke()
        {
            try { Action(); }
            catch (Exception e)
            {
                LoggingManager.Instance.Error($"Failed to Invoke {DisplayName} - {Id} - {Description}");
                Debug.LogException(e);
            }
        }
    }
}
