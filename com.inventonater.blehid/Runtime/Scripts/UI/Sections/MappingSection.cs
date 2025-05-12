using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater
{
    public class MappingSection : SectionUI
    {
        public override string TabName => "Mapping";

        private List<SectionUI> Tabs => _inputRouter.Mappings.Select(mapping => new InputDeviceMappingUI(mapping)).ToList<SectionUI>();

        private readonly InputRouter _inputRouter;
        private readonly TabGroup _tabGroup;
        public MappingSection()
        {
            _inputRouter = GameObject.FindFirstObjectByType<InputRouter>();
            _tabGroup = new("Mappings", Tabs);
        }
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
