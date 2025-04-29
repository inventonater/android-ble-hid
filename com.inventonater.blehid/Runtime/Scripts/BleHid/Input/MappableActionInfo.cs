using System;
using System.Reflection;

namespace Inventonater.BleHid
{
    public class MappableActionInfo
    {
        public EInputAction Id { get; }
        public string DisplayName { get; }
        public string Description { get; }
        public MethodInfo Method { get; }
        public object Target { get; }
        
        public MappableActionInfo(EInputAction id, string displayName, string description, MethodInfo method, object target)
        {
            Id = id;
            DisplayName = displayName;
            Description = description;
            Method = method;
            Target = target;
        }
        
        public void Invoke()
        {
            try 
            { 
                Method.Invoke(Target, null);
            }
            catch (Exception e) 
            { 
                LoggingManager.Instance.Exception(e); 
            }
        }
    }
}
