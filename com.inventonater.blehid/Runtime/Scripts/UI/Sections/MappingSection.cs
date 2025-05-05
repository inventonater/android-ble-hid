using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater
{
    public class MappingSection : SectionUI
    {
        private readonly TabGroup _tabGroup = new("Mappings", Tabs);
        public override string TabName => "Mapping";
        private static List<SectionUI> Tabs => BleHidManager.Instance.InputRouter.Mappings.Select(mapping => new InputDeviceMappingUI(mapping)).ToList<SectionUI>();

        private float lastUpdate;
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