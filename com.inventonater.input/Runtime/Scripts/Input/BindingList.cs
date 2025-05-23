using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class BindingList : MonoBehaviour
    {
        public InputBinding DefaultBinding { get; private set; }
        public List<InputBinding> Bindings { get; } = new();
        public event Action<InputBinding> WhenBindingAdded = delegate { };

        public void SetDefault(InputBinding binding) => DefaultBinding = binding;

        public InputBinding Get(string bindingName) => Bindings.FirstOrDefault(m => m.Name == bindingName) ?? DefaultBinding;

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
