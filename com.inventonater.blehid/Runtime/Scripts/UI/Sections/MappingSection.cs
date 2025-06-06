using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater
{
    public class MappingSection : SectionUI
    {
        public override string TabName => "Mapping";
        private float lastUpdate;
        private readonly BindingList _bindingList;

        private List<SectionUI> Tabs => _bindingList.Bindings.Select(mapping => new InputDeviceMappingUI(mapping)).ToList<SectionUI>();

        private readonly InputRouter _inputRouter;
        private readonly TabGroup _tabGroup;
        public MappingSection(BindingList bindingList)
        {
            _bindingList = bindingList;
            _tabGroup = new("Mappings", Tabs);
        }

        public override void Update()
        {
            if (Time.time - lastUpdate > 1)
            {
                lastUpdate = Time.time;
                _tabGroup.UpdateTabs(Tabs);
            }

            _tabGroup.Update();
        }

        public override void DrawUI()
        {
            _tabGroup.DrawUI();
        }

    }
}
