using System;
using System.Collections.Generic;

namespace Inventonater.BleHid
{
    public class ActionResolver
    {
        private readonly BleBridge _bleBridge;
        private readonly ActionRegistry _registry;
        
        public ActionResolver(BleBridge bleBridge)
        {
            _bleBridge = bleBridge;
            _registry = new ActionRegistry(bleBridge);
        }
        
        public Action ResolveAction(string actionPath, Dictionary<string, object> parameters = null)
        {
            return _registry.GetAction(actionPath, parameters);
        }
        
        public Action ResolveButtonAction(string inputEvent, string action, string keyCode = null)
        {
            var parameters = new Dictionary<string, object>();
            if (!string.IsNullOrEmpty(keyCode))
            {
                parameters["keyCode"] = keyCode;
            }
            
            return ResolveAction(action, parameters);
        }
        
        public Action ResolveDirectionAction(string inputDirection, string action, string keyCode = null)
        {
            var parameters = new Dictionary<string, object>();
            if (!string.IsNullOrEmpty(keyCode))
            {
                parameters["keyCode"] = keyCode;
            }
            
            return ResolveAction(action, parameters);
        }
    }
}
