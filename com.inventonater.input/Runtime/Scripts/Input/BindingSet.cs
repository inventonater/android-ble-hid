using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class BindingSet : MonoBehaviour
    {
        public List<InputBinding> Bindings { get; } = new();
        public event Action<InputBinding> WhenBindingAdded = delegate { };

        public InputBinding Get(string bindingName) => Bindings.First(m => m.Name == bindingName);
        public void Add(InputBinding binding)
        {
            if (Bindings.Any(m => m.Name == binding.Name)) return;
            Bindings.Add(binding);
            WhenBindingAdded(binding);
        }

        public InputBinding CycleNextBinding(InputBinding binding)
        {
            int currentIndex = Bindings.IndexOf(binding);
            int nextIndex = (currentIndex + 1) % Bindings.Count;
            return Bindings[nextIndex];
        }
    }
}
